import React from 'react';
import clsx from 'clsx';
import Link from '@docusaurus/Link';
import useBaseUrl from '@docusaurus/useBaseUrl';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import Layout from '@theme/Layout';
import styles from './index.module.css';

const GITHUB = 'https://github.com/lxien/orbien';
const DEMO = 'https://stackoak.com/';
const DISCORD = 'https://discord.com/invite/4dgQjCS3k';
const RELEASES = 'https://github.com/lxien/orbien/releases';

const features: Array<{ title: string; details: string; to: string }> = [
    {
        title: '多协议代理',
        details: '支持 TCP / UDP / HTTP / HTTPS / SOCKS5 与文件共享',
        to: '/docs/proxies',
    },
    {
        title: '高性能',
        details: '传输层支持 TCP、WebSocket、QUIC协议\n零拷贝、多路复用、压缩、限流',
        to: '/docs/transport/protocols',
    },
    {
        title: '安全',
        details: '隧道 TLS / mTLS 加密传输，Token身份认证，IP CIDR / 时间窗口访问限制、BasicAuth认证',
        to: '/docs/security/access-token',
    },
    {
        title: '请求重放',
        details: '能捕获 HTTP / HTTPS 请求，支持向上游原样重放、编辑后再重放',
        to: '/docs/console/inspector',
    },
    {
        title: '域名证书',
        details: '集成ACME，通过管理面板实现 TLS 证书签发、部署、续期，内置常见云厂商DNS',
        to: '/docs/console',
    },
    {
        title: '负载均衡',
        details: '支持 轮询 / 加权 / 随机 / 最少连接负载均衡，健康检查自动摘除故障节点',
        to: '/docs/reliability/load-balancing',
    },
    {
        title: '可视化运维',
        details: '内置 Web 管理面板，集中配置管理与指标监控\n自带轻量NAS文件面板，支持权限控制',
        to: '/docs/console',
    },
    {
        title: '跨平台',
        details: '支持 Windows / MacOS / Linux 操作系统\n兼容 ARM / X86架构',
        to: '/docs/console',
    },
    {
        title: '简单易用',
        details: '客户端支持 命令行、配置文件、Spring Boot 嵌入式 使用',
        to: '/docs/integrations/cli',
    },
];

function HeroVisual() {
    const dashboardUrl = useBaseUrl('img/dashboard.png');
    const dashboardBlackUrl = useBaseUrl('img/dashboard_black.png');
    return (
        <div className={styles.heroImage}>
            <img
                src={dashboardUrl}
                alt="Orbien 控制台预览"
                className={`${styles.heroImageImg} ${styles.heroImageLight}`}
                loading="lazy"
            />
            <img
                src={dashboardBlackUrl}
                alt="Orbien 控制台预览"
                className={`${styles.heroImageImg} ${styles.heroImageDark}`}
                loading="lazy"
            />
        </div>
    );
}

export default function Home() {
    const {siteConfig} = useDocusaurusContext();

    return (
        <Layout
            title={siteConfig.title}
            description="一个开箱即用的隧道与反向代理平台"
        >
            <div className={styles.page}>
                <section className={styles.hero}>
                    <div className={styles.heroInner}>
                        <div className={styles.heroCopy}>
                            <h1 className={styles.heroName}>
                                <span className={styles.heroBrand}>Orb</span>
                                <span className={styles.heroSuffix}>ien</span>
                            </h1>
                            <p className={styles.heroTagline}>
                                一个开箱即用的隧道与反向代理平台
                            </p>
                            <div className={styles.heroActions}>
                                <Link className={clsx(styles.btn, styles.btnBrand)} to="/docs/getting-started">
                                    快速开始
                                </Link>
                                <Link className={clsx(styles.btn, styles.btnAlt)} href={RELEASES}>
                                    下载
                                </Link>
                                <Link className={clsx(styles.btn, styles.btnAlt)} href={DEMO}>
                                    在线演示
                                </Link>
                                <Link className={clsx(styles.btn, styles.btnGithub)} href={GITHUB}>
                                    GitHub
                                </Link>
                            </div>
                        </div>
                        <HeroVisual/>
                    </div>
                </section>

                <section className={styles.features} aria-label="功能特性">
                    <div className={styles.featuresInner}>
                        <ul className={styles.featureGrid}>
                            {features.map((f) => (
                                <li key={f.title} className={styles.featureItem}>
                                    <Link className={styles.featureLink} to={f.to}>
                                        <h3 className={styles.featureTitle}>{f.title}</h3>
                                        <p className={styles.featureDetails}>{f.details}</p>
                                    </Link>
                                </li>
                            ))}
                        </ul>
                    </div>
                </section>

                <section className={styles.linksSection} aria-labelledby="home-links-title">
                    <div className={styles.linksInner}>
                        <h2 id="home-links-title" className={styles.linksTitle}>
                            相关链接
                        </h2>
                        <ul className={styles.linksList}>
                            <li>
                                Discord：
                                <a href={DISCORD} target="_blank" rel="noopener noreferrer">
                                    {DISCORD}
                                </a>
                            </li>
                            <li>
                                问题反馈：
                                <a
                                    href="https://github.com/lxien/orbien/issues"
                                    target="_blank"
                                    rel="noopener noreferrer"
                                >
                                    https://github.com/lxien/orbien/issues
                                </a>
                            </li>
                        </ul>
                    </div>
                </section>
            </div>
        </Layout>
    );
}
