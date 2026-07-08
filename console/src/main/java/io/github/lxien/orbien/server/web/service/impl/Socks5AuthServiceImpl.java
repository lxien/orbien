package io.github.lxien.orbien.server.web.service.impl;

import io.github.lxien.orbien.server.web.common.exception.BizException;
import io.github.lxien.orbien.server.web.dto.socks5auth.Socks5AuthDetailDTO;
import io.github.lxien.orbien.server.web.entity.Socks5AuthDO;
import io.github.lxien.orbien.server.web.entity.Socks5UserDO;
import io.github.lxien.orbien.server.web.param.socks5auth.Socks5AuthUpdateParam;
import io.github.lxien.orbien.server.web.param.socks5auth.Socks5UserAddParam;
import io.github.lxien.orbien.server.web.param.socks5auth.Socks5UserUpdateParam;
import io.github.lxien.orbien.server.web.proxy.service.ProxyRuntimeSyncService;
import io.github.lxien.orbien.server.web.repository.Socks5AuthRepository;
import io.github.lxien.orbien.server.web.repository.Socks5UserRepository;
import io.github.lxien.orbien.server.web.service.Socks5AuthService;
import io.github.lxien.orbien.server.web.service.converter.Socks5AuthConvert;
import io.github.lxien.orbien.server.web.support.tx.TransactionHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class Socks5AuthServiceImpl implements Socks5AuthService {

    @Autowired
    private Socks5AuthRepository socks5AuthRepository;
    @Autowired
    private Socks5UserRepository socks5UserRepository;
    @Autowired
    private Socks5AuthConvert socks5AuthConvert;
    @Autowired
    private TransactionHelper transactionHelper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ProxyRuntimeSyncService proxyRuntimeSyncService;

    @Override
    public Socks5AuthDetailDTO getByProxyId(String proxyId) {
        Socks5AuthDO authDO = socks5AuthRepository.findById(proxyId)
                .orElseThrow(() -> new BizException("SOCKS5 认证配置不存在"));
        List<Socks5UserDO> users = socks5UserRepository.findByProxyId(proxyId);
        return socks5AuthConvert.toDetailDTO(authDO, users);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Socks5AuthUpdateParam request) {
        String proxyId = request.getProxyId();
        Socks5AuthDO authDO = socks5AuthRepository.findById(proxyId)
                .orElseThrow(() -> new BizException("SOCKS5 认证配置不存在"));
        authDO.setEnabled(request.getEnabled());
        socks5AuthRepository.save(authDO);
        scheduleRefresh(proxyId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addUser(Socks5UserAddParam request) {
        String proxyId = request.getProxyId();
        socks5AuthRepository.findById(proxyId)
                .orElseThrow(() -> new BizException("SOCKS5 认证配置不存在"));
        if (socks5UserRepository.existsByProxyIdAndUsername(proxyId, request.getUsername())) {
            throw new BizException("用户名已存在");
        }
        Socks5UserDO userDO = socks5AuthConvert.toUserDO(request);
        userDO.setPassword(passwordEncoder.encode(request.getPassword()));
        socks5UserRepository.save(userDO);
        scheduleRefresh(proxyId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(Socks5UserUpdateParam param) {
        String proxyId = param.getProxyId();
        Socks5UserDO userDO = socks5UserRepository.findById(param.getId())
                .orElseThrow(() -> new BizException("用户不存在"));
        if (!Objects.equals(param.getUsername(), userDO.getUsername())
                && socks5UserRepository.existsByProxyIdAndUsernameAndIdNot(proxyId, param.getUsername(), param.getId())) {
            throw new BizException("用户名已存在");
        }
        socks5AuthConvert.updateUserDO(userDO, param);
        userDO.setPassword(passwordEncoder.encode(param.getPassword()));
        socks5UserRepository.save(userDO);
        scheduleRefresh(proxyId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        Socks5UserDO userDO = socks5UserRepository.findById(id)
                .orElseThrow(() -> new BizException("用户不存在"));
        String proxyId = userDO.getProxyId();
        socks5UserRepository.deleteById(id);
        scheduleRefresh(proxyId);
    }

    private void scheduleRefresh(String proxyId) {
        transactionHelper.afterCommit(() -> proxyRuntimeSyncService.refreshServerEntryPolicy(proxyId));
    }
}
