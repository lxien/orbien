(() => {
    'use strict';

    const ICON_BASE = '/icon/';
    const ICON_FILES = {
        folder: 'folder.svg',
        file: 'file.svg',
        image: 'image.svg',
        video: 'video.svg',
        audio: 'audio.svg',
        word: 'word.svg',
        pdf: 'pdf.svg',
        ppt: 'powerpoint.svg',
        excel: 'excel.svg',
        csv: 'csv.svg',
        archive: 'archive.svg',
        html: 'html.svg',
        css: 'css.svg',
        java: 'java.svg',
        markdown: 'markdown.svg',
        xml: 'xml.svg',
        yaml: 'yaml.svg',
        photoshop: 'photoshop.svg',
        apk: 'apk.svg',
        exe: 'exe.svg',
        dmg: 'dmg.svg'
    };

    const EXT_ICON_TYPES = {
        jpg: 'image', jpeg: 'image', png: 'image', gif: 'image', bmp: 'image',
        webp: 'image', svg: 'image', ico: 'image', tiff: 'image', tif: 'image',
        heic: 'image', heif: 'image', raw: 'image', psd: 'photoshop',
        mp4: 'video', avi: 'video', mkv: 'video', mov: 'video', wmv: 'video',
        flv: 'video', webm: 'video', m4v: 'video', mpg: 'video', mpeg: 'video',
        ts: 'video', m2ts: 'video', mts: 'video', '3gp': 'video',
        mp3: 'audio', wav: 'audio', flac: 'audio', aac: 'audio', ogg: 'audio',
        m4a: 'audio', wma: 'audio', aiff: 'audio', ape: 'audio',
        doc: 'word', docx: 'word', rtf: 'word', odt: 'word',
        pdf: 'pdf',
        ppt: 'ppt', pptx: 'ppt', pps: 'ppt', ppsx: 'ppt',
        xls: 'excel', xlsx: 'excel', xlsm: 'excel', xlsb: 'excel',
        csv: 'csv',
        zip: 'archive', rar: 'archive', '7z': 'archive', tar: 'archive',
        gz: 'archive', bz2: 'archive', xz: 'archive', tgz: 'archive', tbz: 'archive',
        jar: 'java', java: 'java', class: 'java',
        html: 'html', htm: 'html', xhtml: 'html',
        css: 'css', scss: 'css', sass: 'css', less: 'css',
        md: 'markdown', markdown: 'markdown',
        xml: 'xml', xsd: 'xml', xsl: 'xml', xslt: 'xml',
        yaml: 'yaml', yml: 'yaml', json: 'yaml',
        js: 'java', mjs: 'java', cjs: 'java', jsx: 'java',
        ts: 'java', tsx: 'java', es: 'java',
        vue: 'html', svelte: 'html',
        apk: 'apk', exe: 'exe', msi: 'exe', bat: 'exe', cmd: 'exe',
        dmg: 'dmg', pkg: 'dmg', deb: 'dmg', rpm: 'dmg', iso: 'dmg'
    };

    const THEME_KEY = 'orbien-file-theme';
    const THEME_MODES = ['light', 'dark', 'system'];
    const THEME_LABELS = {
        light: '白昼模式',
        dark: '夜间模式',
        system: '跟随系统'
    };
    const THEME_NEXT_HINT = {
        light: '切换到夜间模式',
        dark: '切换到跟随系统',
        system: '切换到白昼模式'
    };
    const VIEW_KEY = 'orbien-file-view';
    const AUTH_REFRESH_INTERVAL = 30_000;
    const VIEW_MODES = ['list', 'icon', 'column'];
    const WRITE_CONTROL_IDS = ['btnUpload', 'btnMkdir', 'btnDelete', 'btnRename', 'fileInput', 'emptyUploadLink', 'emptyMkdirLink'];
    const THEME_BUTTON_IDS = ['btnTheme', 'btnThemeLogin'];

    let currentPath = '/';
    let entries = [];
    let authRequired = true;
    let canWrite = true;
    let canMove = true;
    let canRename = true;
    let permission = 'read_write';
    let currentUsername = '';
    let authRefreshTimer = null;
    let listLoading = false;
    let viewMode = 'list';
    let dragPaths = [];
    const selectedPathSet = new Set();
    let selectionAnchorPath = null;
    const entriesCache = new Map();

    const $ = (id) => document.getElementById(id);

    const isMultiSelectModifier = (e) => !!(e.ctrlKey || e.metaKey);
    const isRangeSelectModifier = (e) => !!e.shiftKey;

    const clearSelection = () => {
        selectedPathSet.clear();
        selectionAnchorPath = null;
    };

    const pruneSelection = () => {
        const validPaths = new Set(entries.map((entry) => entryPath(entry)));
        for (const path of [...selectedPathSet]) {
            if (!validPaths.has(path)) {
                selectedPathSet.delete(path);
            }
        }
        if (selectionAnchorPath && !validPaths.has(selectionAnchorPath)) {
            selectionAnchorPath = null;
        }
    };

    const orderedCurrentPaths = () => entries.map((entry) => entryPath(entry));

    const applySelectionClick = (e, index, path, orderedPaths = orderedCurrentPaths()) => {
        const shift = isRangeSelectModifier(e);
        const multi = isMultiSelectModifier(e);

        if (shift && selectionAnchorPath !== null) {
            const anchorIndex = orderedPaths.indexOf(selectionAnchorPath);
            if (anchorIndex === -1) {
                if (!multi) {
                    selectedPathSet.clear();
                }
                selectedPathSet.add(path);
            } else {
                const start = Math.min(anchorIndex, index);
                const end = Math.max(anchorIndex, index);
                if (!multi) {
                    selectedPathSet.clear();
                }
                for (let i = start; i <= end; i++) {
                    selectedPathSet.add(orderedPaths[i]);
                }
            }
        } else if (multi) {
            if (selectedPathSet.has(path)) {
                selectedPathSet.delete(path);
            } else {
                selectedPathSet.add(path);
            }
            selectionAnchorPath = path;
        } else {
            selectedPathSet.clear();
            selectedPathSet.add(path);
            selectionAnchorPath = path;
        }
        syncSelectionState();
    };

    const entryName = (entry) => entry.name || entry.name_ || '';

    const joinPath = (base, name) => (base === '/' ? `/${name}` : `${base}/${name}`);

    const iconUrl = (type) => ICON_BASE + encodeURIComponent(ICON_FILES[type] || ICON_FILES.file);

    const fileExtension = (name) => {
        const dot = name.lastIndexOf('.');
        if (dot <= 0 || dot === name.length - 1) {
            return '';
        }
        return name.slice(dot + 1).toLowerCase();
    };

    const resolveIconType = (name, isDirectory) => {
        if (isDirectory) {
            return 'folder';
        }
        const ext = fileExtension(name);
        return ext ? (EXT_ICON_TYPES[ext] || 'file') : 'file';
    };

    const createFileIcon = (name, isDirectory, large = false) => {
        const img = document.createElement('img');
        img.className = large ? 'file-icon file-icon-lg' : 'file-icon';
        img.alt = '';
        img.src = iconUrl(resolveIconType(name, isDirectory));
        img.onerror = () => {
            img.onerror = null;
            img.src = iconUrl('file');
        };
        return img;
    };

    const fileTypeLabel = (name, isDirectory) => {
        if (isDirectory) {
            return '文件夹';
        }
        const ext = fileExtension(name);
        return ext ? ext.toUpperCase() : '文件';
    };

    const escapeHtml = (text) => String(text)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');

    const formatSize = (n) => {
        if (!n) return '';
        if (n < 1024) return `${n} B`;
        if (n < 1_048_576) return `${(n / 1024).toFixed(1)} KB`;
        return `${(n / 1_048_576).toFixed(1)} MB`;
    };

    const formatTime = (ms) => (ms ? new Date(Number(ms)).toLocaleString('zh-CN') : '');

    const permissionLabel = (value) => (value === 'read_write' ? '读写' : '只读');

    const extractErrorMessage = (data, fallback) => {
        if (!data) return fallback;
        if (data.message) return data.message;
        if (data.status?.message) return data.status.message;
        return fallback;
    };

    let dialogResolver = null;
    let dialogOptions = null;
    let dialogKeyHandler = null;

    const closeDialog = (action, value) => {
        const root = $('appDialog');
        if (!root || root.classList.contains('hidden')) {
            return;
        }
        root.classList.add('hidden');
        root.setAttribute('aria-hidden', 'true');
        document.body.classList.remove('dialog-open');
        if (dialogKeyHandler) {
            document.removeEventListener('keydown', dialogKeyHandler);
            dialogKeyHandler = null;
        }
        const resolve = dialogResolver;
        dialogResolver = null;
        dialogOptions = null;
        if (resolve) {
            resolve({action, value});
        }
    };

    const openDialog = (options = {}) => new Promise((resolve) => {
        const root = $('appDialog');
        const icon = $('dialogIcon');
        const titleEl = $('dialogTitle');
        const messageEl = $('dialogMessage');
        const inputWrap = $('dialogInputWrap');
        const input = $('dialogInput');
        const btnCancel = $('dialogCancel');
        const btnConfirm = $('dialogConfirm');
        if (!root || !icon || !titleEl || !messageEl || !btnCancel || !btnConfirm || !input || !inputWrap) {
            resolve({action: 'cancel'});
            return;
        }

        if (dialogResolver) {
            closeDialog('cancel');
        }

        const {
            title = '提示',
            message = '',
            type = 'info',
            confirmText = '确定',
            cancelText = '取消',
            showCancel = true,
            danger = false,
            prompt = false,
            defaultValue = '',
            placeholder = '',
            dismissOnBackdrop = true,
            selectOnOpen = true
        } = options;

        dialogResolver = resolve;
        dialogOptions = {prompt, dismissOnBackdrop, danger, showCancel};

        icon.className = `app-dialog-icon is-${type}`;
        titleEl.textContent = title;
        messageEl.textContent = message;
        messageEl.classList.toggle('hidden', !message);

        inputWrap.classList.toggle('hidden', !prompt);
        input.value = defaultValue;
        input.placeholder = placeholder;

        btnCancel.textContent = cancelText;
        btnConfirm.textContent = confirmText;
        btnCancel.classList.toggle('hidden', !showCancel);
        btnConfirm.classList.toggle('danger', danger);
        btnConfirm.classList.toggle('primary', !danger);

        root.classList.remove('hidden');
        root.setAttribute('aria-hidden', 'false');
        document.body.classList.add('dialog-open');

        dialogKeyHandler = (e) => {
            if (e.key === 'Escape') {
                e.preventDefault();
                closeDialog('cancel');
                return;
            }
            if (e.key === 'Enter' && !dialogOptions?.prompt) {
                if (dialogOptions?.danger && dialogOptions?.showCancel) {
                    return;
                }
                e.preventDefault();
                closeDialog('confirm');
                return;
            }
            if (e.key === 'Enter' && prompt && document.activeElement === input) {
                e.preventDefault();
                closeDialog('confirm', input.value);
            }
        };
        document.addEventListener('keydown', dialogKeyHandler);

        requestAnimationFrame(() => {
            if (prompt) {
                input.focus();
                if (selectOnOpen) {
                    input.select();
                }
                return;
            }
            if (danger && showCancel) {
                btnCancel.focus();
                return;
            }
            (showCancel ? btnCancel : btnConfirm).focus();
        });
    });

    const showAlert = (message, options = {}) => openDialog({
        title: options.title || '提示',
        message,
        type: options.type || 'info',
        confirmText: options.confirmText || '知道了',
        showCancel: false,
        dismissOnBackdrop: options.dismissOnBackdrop !== false
    }).then(() => undefined);

    const showConfirm = (messageOrOptions, legacyOptions = {}) => {
        const options = typeof messageOrOptions === 'string'
            ? {...legacyOptions, message: messageOrOptions}
            : messageOrOptions;
        return openDialog({
            title: options.title || '确认',
            message: options.message || '',
            type: options.type || (options.danger ? 'danger' : 'warning'),
            confirmText: options.confirmText || '确定',
            cancelText: options.cancelText || '取消',
            danger: !!options.danger,
            showCancel: true
        });
    };

    const showPrompt = (options = {}) => openDialog({
        title: options.title || '请输入',
        message: options.message || '',
        type: 'prompt',
        confirmText: options.confirmText || '确定',
        cancelText: options.cancelText || '取消',
        prompt: true,
        defaultValue: options.defaultValue || '',
        placeholder: options.placeholder || '',
        selectOnOpen: options.selectOnOpen !== false
    });

    const initDialog = () => {
        const root = $('appDialog');
        if (!root) {
            return;
        }
        root.querySelectorAll('[data-dialog-dismiss="true"]').forEach((el) => {
            el.addEventListener('click', () => {
                if (dialogOptions?.dismissOnBackdrop !== false) {
                    closeDialog('cancel');
                }
            });
        });
        $('dialogCancel')?.addEventListener('click', () => closeDialog('cancel'));
        $('dialogConfirm')?.addEventListener('click', () => {
            const input = $('dialogInput');
            closeDialog('confirm', dialogOptions?.prompt ? input?.value : undefined);
        });
    };

    const showError = async (err) => {
        await showAlert(err.message || String(err), {title: '操作失败', type: 'danger'});
    };

    const sortEntries = (list) => [...list].sort((a, b) => {
        const aDir = !!a.directory;
        const bDir = !!b.directory;
        if (aDir !== bDir) {
            return aDir ? -1 : 1;
        }
        return entryName(a).localeCompare(entryName(b), 'zh-CN');
    });

    const pathChain = (path) => {
        if (path === '/') {
            return ['/'];
        }
        const parts = path.split('/').filter(Boolean);
        const chain = ['/'];
        let acc = '';
        for (const part of parts) {
            acc += `/${part}`;
            chain.push(acc);
        }
        return chain;
    };

    const selectedChildName = (parentPath, childPath) => {
        if (parentPath === '/') {
            return childPath.split('/').filter(Boolean)[0] || null;
        }
        const prefix = `${parentPath}/`;
        if (!childPath.startsWith(prefix)) {
            return null;
        }
        return childPath.slice(prefix.length).split('/').filter(Boolean)[0] || null;
    };

    const invalidateEntriesCache = () => entriesCache.clear();

    const normalizeThemeMode = (mode) => (THEME_MODES.includes(mode) ? mode : 'light');

    const resolveEffectiveTheme = (mode) => {
        if (mode === 'system') {
            return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
        }
        return mode === 'dark' ? 'dark' : 'light';
    };

    const applyTheme = (mode) => {
        const themeMode = normalizeThemeMode(mode);
        const effective = resolveEffectiveTheme(themeMode);
        document.documentElement.setAttribute('data-theme', effective);
        document.documentElement.setAttribute('data-theme-mode', themeMode);
        localStorage.setItem(THEME_KEY, themeMode);
        const title = `${THEME_LABELS[themeMode]} · ${THEME_NEXT_HINT[themeMode]}`;
        for (const id of THEME_BUTTON_IDS) {
            const btn = $(id);
            if (!btn) {
                continue;
            }
            btn.dataset.themeMode = themeMode;
            btn.title = title;
            btn.setAttribute('aria-label', title);
        }
    };

    const initTheme = () => {
        const saved = normalizeThemeMode(localStorage.getItem(THEME_KEY));
        applyTheme(saved);
        const media = window.matchMedia('(prefers-color-scheme: dark)');
        const onSystemThemeChange = () => {
            if (normalizeThemeMode(localStorage.getItem(THEME_KEY)) === 'system') {
                document.documentElement.setAttribute('data-theme', resolveEffectiveTheme('system'));
            }
        };
        if (typeof media.addEventListener === 'function') {
            media.addEventListener('change', onSystemThemeChange);
        } else if (typeof media.addListener === 'function') {
            media.addListener(onSystemThemeChange);
        }
    };

    const toggleTheme = () => {
        const current = normalizeThemeMode(localStorage.getItem(THEME_KEY));
        const index = THEME_MODES.indexOf(current);
        applyTheme(THEME_MODES[(index + 1) % THEME_MODES.length]);
    };

    const updatePermissionBadge = () => {
        const badge = $('permissionBadge');
        const readonlyTip = $('readonlyTip');
        if (!authRequired) {
            badge.classList.add('hidden');
            readonlyTip.classList.add('hidden');
            return;
        }
        badge.classList.remove('hidden');
        badge.textContent = permissionLabel(permission);
        badge.className = `permission-badge ${canWrite ? 'write' : 'read'}`;
        readonlyTip.classList.toggle('hidden', canWrite);
    };

    const applyCapabilities = (data) => {
        canWrite = data.canWrite === true;
        canMove = data.canMove !== false && canWrite;
        canRename = data.canRename !== false && canWrite;
    };

    const updateWriteControls = () => {
        for (const id of WRITE_CONTROL_IDS) {
            const el = $(id);
            if (el) {
                el.style.display = canWrite ? '' : 'none';
            }
        }
        const renameBtn = $('btnRename');
        if (renameBtn) {
            renameBtn.style.display = canRename ? '' : 'none';
        }
        const emptyActions = document.querySelector('.empty-actions');
        const emptyDesc = document.querySelector('.empty-desc');
        if (emptyActions) {
            emptyActions.style.display = canWrite ? '' : 'none';
        }
        if (emptyDesc) {
            emptyDesc.textContent = canWrite
                ? '开始添加内容，让文件管理更高效'
                : '当前目录暂无文件';
        }
    };

    const showLogin = (message) => {
        $('loginView').classList.remove('hidden');
        $('appView').classList.add('hidden');
        const errEl = $('loginError');
        if (message) {
            errEl.textContent = message;
            errEl.style.display = 'block';
        } else {
            errEl.style.display = 'none';
        }
    };

    const showApp = (username) => {
        $('loginView').classList.add('hidden');
        $('appView').classList.remove('hidden');
        $('currentUser').textContent = username ? `${username} ` : '';
        $('btnLogout').classList.toggle('hidden', !authRequired);
        updatePermissionBadge();
        updateWriteControls();
    };

    async function api(path, options = {}) {
        const res = await fetch(path, {credentials: 'include', ...options});
        const ct = res.headers.get('content-type') || '';
        if (ct.includes('application/json')) {
            const data = await res.json();
            if (res.status === 401) {
                if (authRequired) {
                    showLogin(extractErrorMessage(data, '未登录'));
                }
                throw new Error(extractErrorMessage(data, '未登录'));
            }
            if (res.status === 403) {
                refreshAuthStatus().catch(() => { /* ignore */
                });
                throw new Error(extractErrorMessage(data, '没有操作权限'));
            }
            if (!res.ok) {
                throw new Error(extractErrorMessage(data, `请求失败: ${res.status}`));
            }
            if (data.status && data.status.code !== 0) {
                throw new Error(extractErrorMessage(data, '操作失败'));
            }
            return data;
        }
        if (!res.ok) {
            throw new Error(`请求失败: ${res.status}`);
        }
        return res;
    }

    async function refreshAuthStatus() {
        const res = await fetch('/api/auth/status', {credentials: 'include'});
        const data = await res.json();
        authRequired = !!data.authRequired;
        permission = data.permission || (data.canWrite ? 'read_write' : 'read');
        applyCapabilities(data);
        currentUsername = data.username || '';

        if (!authRequired) {
            showApp(currentUsername || '访客');
            return data;
        }
        if (data.loggedIn) {
            showApp(currentUsername);
            return data;
        }
        showLogin(data.message || '');
        return data;
    }

    const scheduleAuthRefresh = () => {
        if (authRefreshTimer) {
            clearInterval(authRefreshTimer);
        }
        authRefreshTimer = setInterval(() => {
            refreshAuthStatus().catch(() => { /* ignore background refresh errors */
            });
        }, AUTH_REFRESH_INTERVAL);
    };

    async function login() {
        const username = $('username').value.trim();
        const password = $('password').value;
        const errEl = $('loginError');
        errEl.style.display = 'none';
        try {
            const data = await api('/api/auth/login', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({username, password})
            });
            canWrite = data.canWrite === true;
            applyCapabilities(data);
            permission = data.permission || (canWrite ? 'read_write' : 'read');
            currentUsername = username;
            showApp(username);
            await loadList();
        } catch (e) {
            errEl.textContent = e.message || '登录失败';
            errEl.style.display = 'block';
        }
    }

    async function logout() {
        try {
            await api('/api/auth/logout', {method: 'POST'});
        } catch { /* ignore */
        }
        canWrite = false;
        canMove = false;
        canRename = false;
        permission = 'read';
        showLogin();
    }

    async function ensureWritable(actionName) {
        await refreshAuthStatus();
        if (!canWrite) {
            throw new Error(`当前为只读权限，无法${actionName || '执行此操作'}`);
        }
    }

    const parentPath = (path) => {
        if (path === '/') {
            return '/';
        }
        const parts = path.split('/').filter(Boolean);
        parts.pop();
        return parts.length ? `/${parts.join('/')}` : '/';
    };

    const setListLoading = (loading) => {
        listLoading = loading;
        $('fileListWrap').classList.toggle('is-loading', loading);
    };

    const updatePathNav = () => {
        const atRoot = currentPath === '/';
        $('btnBack').classList.toggle('hidden', atRoot);
    };

    const applyViewMode = (mode, persist = true) => {
        if (!VIEW_MODES.includes(mode)) {
            mode = 'list';
        }
        viewMode = mode;
        if (persist) {
            localStorage.setItem(VIEW_KEY, mode);
        }

        $('fileListWrap').dataset.viewMode = mode;
        $('mainPanel').classList.toggle('is-column-view', mode === 'column');

        $('viewList').classList.toggle('hidden', mode !== 'list');
        $('viewIcon').classList.toggle('hidden', mode !== 'icon');
        $('viewColumn').classList.toggle('hidden', mode !== 'column');

        for (const btn of document.querySelectorAll('.view-btn')) {
            const active = btn.dataset.view === mode;
            btn.classList.toggle('active', active);
            btn.setAttribute('aria-pressed', active ? 'true' : 'false');
        }
    };

    const initViewMode = () => {
        const saved = localStorage.getItem(VIEW_KEY);
        applyViewMode(VIEW_MODES.includes(saved) ? saved : 'list', false);
    };

    const setViewMode = (mode) => {
        if (mode === viewMode) {
            return;
        }
        if (mode !== 'column') {
            clearSelection();
        }
        applyViewMode(mode);
        renderFiles().catch(showError);
    };

    const navigateTo = (path) => {
        if (listLoading || path === currentPath) {
            return;
        }
        clearSelection();
        currentPath = path;
        loadList().catch(showError);
    };

    const goBack = () => navigateTo(parentPath(currentPath));

    const goHome = () => navigateTo('/');

    async function fetchEntries(path, useCache = true) {
        if (useCache && entriesCache.has(path)) {
            return entriesCache.get(path);
        }
        const data = await api(`/api/files/list?path=${encodeURIComponent(path)}`);
        const list = sortEntries(data.entries || data.entriesList || []);
        entriesCache.set(path, list);
        return list;
    }

    async function loadList(skipGuard = false) {
        if (!skipGuard && listLoading) {
            return;
        }
        setListLoading(true);
        try {
            invalidateEntriesCache();
            entries = await fetchEntries(currentPath, false);
            pruneSelection();
            await renderFiles();
        } catch (e) {
            if (e.message?.includes('目录不存在') || e.message?.includes('不是目录')) {
                if (currentPath !== '/') {
                    currentPath = parentPath(currentPath);
                    await loadList(true);
                    return;
                }
            }
            throw e;
        } finally {
            setListLoading(false);
        }
    }

    const renderBreadcrumb = () => {
        const nav = $('breadcrumb');
        nav.replaceChildren();
        updatePathNav();

        const parts = currentPath === '/' ? [] : currentPath.split('/').filter(Boolean);
        const items = [{label: '全部文件', path: '/'}];
        let acc = '';
        for (const part of parts) {
            acc += `/${part}`;
            items.push({label: part, path: acc});
        }

        items.forEach((item, index) => {
            if (index > 0) {
                const sep = document.createElement('span');
                sep.className = 'breadcrumb-sep';
                sep.textContent = '/';
                sep.setAttribute('aria-hidden', 'true');
                nav.appendChild(sep);
            }

            const wrap = document.createElement('span');
            wrap.className = 'breadcrumb-item';

            const isLast = index === items.length - 1;
            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'breadcrumb-link';
            if (index === 0) {
                btn.classList.add('is-root');
            }
            if (isLast) {
                btn.classList.add('is-current');
            }
            btn.textContent = item.label;
            btn.title = item.label;
            btn.dataset.path = item.path;
            wrap.appendChild(btn);
            nav.appendChild(wrap);
        });
        bindBreadcrumbDropTargets();
    };

    const updateSummary = () => {
        const dirs = entries.filter((e) => e.directory).length;
        const files = entries.length - dirs;
        $('summary').textContent = entries.length
            ? `${dirs} 个文件夹，${files} 个文件`
            : '此文件夹为空';
        const showGlobalEmpty = entries.length === 0 && viewMode !== 'column';
        $('emptyState').classList.toggle('hidden', !showGlobalEmpty);
    };

    const createColumnEmptyContent = () => {
        const wrap = document.createElement('div');
        wrap.className = 'column-panel-empty';

        const text = document.createElement('p');
        text.className = 'column-panel-empty-text';
        text.textContent = canWrite ? '此文件夹为空' : '当前目录暂无文件';
        wrap.appendChild(text);

        if (canWrite) {
            const actions = document.createElement('div');
            actions.className = 'column-panel-empty-actions';

            const uploadBtn = document.createElement('button');
            uploadBtn.type = 'button';
            uploadBtn.className = 'empty-action';
            uploadBtn.textContent = '上传文件';
            uploadBtn.addEventListener('click', () => $('fileInput').click());

            const divider = document.createElement('span');
            divider.className = 'empty-divider';
            divider.setAttribute('aria-hidden', 'true');

            const mkdirBtn = document.createElement('button');
            mkdirBtn.type = 'button';
            mkdirBtn.className = 'empty-action';
            mkdirBtn.textContent = '新建文件夹';
            mkdirBtn.addEventListener('click', () => mkdir().catch(showError));

            actions.append(uploadBtn, divider, mkdirBtn);
            wrap.appendChild(actions);
        }
        return wrap;
    };

    const scrollColumnBrowser = (browser) => {
        requestAnimationFrame(() => {
            const selected = browser.querySelector('.column-item.is-selected');
            if (selected) {
                selected.scrollIntoView({block: 'nearest', inline: 'nearest'});
            }
            browser.scrollLeft = Math.max(0, browser.scrollWidth - browser.clientWidth);
        });
    };

    const entryPath = (entry, basePath = currentPath) => joinPath(basePath, entryName(entry));

    const findEntryByPath = (path) => {
        const parent = parentDirOf(path);
        const name = path.split('/').pop();
        const list = parent === currentPath ? entries : entriesCache.get(parent);
        return list?.find((entry) => entryName(entry) === name);
    };

    const syncSelectionState = () => {
        for (const item of document.querySelectorAll('.icon-item')) {
            const path = item.dataset.filePath;
            item.classList.toggle('is-checked', !!path && selectedPathSet.has(path));
        }
        for (const row of document.querySelectorAll('#fileList tr')) {
            const path = row.dataset.filePath;
            row.classList.toggle('is-checked', !!path && selectedPathSet.has(path));
        }
        for (const item of document.querySelectorAll('.column-item')) {
            const path = item.dataset.filePath;
            item.classList.toggle('is-checked', !!path && selectedPathSet.has(path));
        }
    };

    const bindEnterDir = (element, name, options = {}) => {
        const {dblclick = false} = options;
        const open = () => enterDir(name);
        if (dblclick) {
            element.addEventListener('dblclick', open);
        } else {
            element.addEventListener('click', open);
        }
    };

    const parentDirOf = (path) => {
        if (!path || path === '/') {
            return '/';
        }
        const parts = path.split('/').filter(Boolean);
        parts.pop();
        return parts.length ? `/${parts.join('/')}` : '/';
    };

    const canDropMove = (sourcePaths, destDir) => {
        for (const src of sourcePaths) {
            if (!src || src === destDir) {
                return false;
            }
            if (destDir.startsWith(`${src}/`)) {
                return false;
            }
            if (parentDirOf(src) === destDir) {
                return false;
            }
        }
        return true;
    };

    const clearDropTargets = () => {
        for (const el of document.querySelectorAll('.drop-target')) {
            el.classList.remove('drop-target');
        }
    };

    const bindDragSource = (element, filePath) => {
        if (!canMove || !filePath) {
            return;
        }
        element.draggable = true;
        element.addEventListener('dragstart', (e) => {
            const selected = selectedPaths();
            dragPaths = selected.includes(filePath) ? selected : [filePath];
            e.dataTransfer.setData('text/plain', filePath);
            e.dataTransfer.effectAllowed = 'move';
            element.classList.add('is-dragging');
        });
        element.addEventListener('dragend', () => {
            element.classList.remove('is-dragging');
            clearDropTargets();
            dragPaths = [];
        });
    };

    const dropBindings = new WeakMap();

    const bindDropTarget = (element, destDir) => {
        if (!canMove || !destDir) {
            return;
        }
        const prev = dropBindings.get(element);
        if (prev?.destDir === destDir) {
            return;
        }
        if (prev?.controller) {
            prev.controller.abort();
        }
        const controller = new AbortController();
        const {signal} = controller;
        dropBindings.set(element, {destDir, controller});

        element.addEventListener('dragover', (e) => {
            const paths = dragPaths.length ? dragPaths : [];
            if (!paths.length) {
                return;
            }
            if (!canDropMove(paths, destDir)) {
                return;
            }
            e.preventDefault();
            e.stopPropagation();
            e.dataTransfer.dropEffect = 'move';
            clearDropTargets();
            element.classList.add('drop-target');
        }, {signal});

        element.addEventListener('dragleave', (e) => {
            if (!element.contains(e.relatedTarget)) {
                element.classList.remove('drop-target');
            }
        }, {signal});

        element.addEventListener('drop', (e) => {
            e.preventDefault();
            e.stopPropagation();
            element.classList.remove('drop-target');
            const paths = dragPaths.length
                ? dragPaths
                : [e.dataTransfer.getData('text/plain')].filter(Boolean);
            if (!paths.length || !canDropMove(paths, destDir)) {
                return;
            }
            moveItems(paths, destDir).catch(showError);
        }, {signal});
    };

    const bindDirectoryDropZones = () => {
        bindDropTarget($('fileListWrap'), currentPath);
        const pathNavMain = document.querySelector('.path-nav-main');
        if (pathNavMain) {
            bindDropTarget(pathNavMain, currentPath);
        }
        const back = $('btnBack');
        if (back && currentPath !== '/') {
            bindDropTarget(back, parentPath(currentPath));
        }
        for (const panel of document.querySelectorAll('.column-panel')) {
            bindDropTarget(panel, panel.dataset.path);
        }
    };

    const bindBreadcrumbDropTargets = () => {
        for (const btn of document.querySelectorAll('#breadcrumb .breadcrumb-link[data-path]')) {
            bindDropTarget(btn, btn.dataset.path);
        }
    };

    async function ensureMovable() {
        await refreshAuthStatus();
        if (!canMove) {
            throw new Error('当前不允许移动文件');
        }
    }

    async function ensureRenamable() {
        await refreshAuthStatus();
        if (!canRename) {
            throw new Error('当前不允许重命名');
        }
    }

    async function moveItems(paths, destDir) {
        await ensureMovable();
        const items = [...new Set(paths)].filter((p) => p && canDropMove([p], destDir));
        if (!items.length) {
            return;
        }
        const data = await api('/api/files/move', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({items, destDir})
        });
        invalidateEntriesCache();
        clearSelection();
        await loadList();
        if (data.failCount > 0) {
            const lines = (data.results || [])
                .filter((item) => !item.ok)
                .map((item) => `${item.path}: ${item.message || '移动失败'}`);
            await showAlert(
                `成功 ${data.successCount} 项，失败 ${data.failCount} 项：\n${lines.join('\n')}`,
                {title: '部分移动失败', type: 'warning'}
            );
        }
    }

    async function renameSelected() {
        await ensureRenamable();
        const paths = selectedPaths();
        if (paths.length !== 1) {
            await showAlert('请选择一个文件或文件夹进行重命名', {title: '无法重命名', type: 'warning'});
            return;
        }
        const oldPath = paths[0];
        const oldName = oldPath.split('/').pop() || '';
        const result = await showPrompt({
            title: '重命名',
            message: '请输入新名称',
            defaultValue: oldName,
            confirmText: '确定',
            cancelText: '取消'
        });
        if (result.action !== 'confirm') {
            return;
        }
        const trimmed = (result.value || '').trim();
        if (!trimmed || trimmed === oldName) {
            return;
        }
        await api('/api/files/rename', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({path: oldPath, name: trimmed})
        });
        if (viewMode === 'column' && selectedPathSet.size === 1) {
            const onlyPath = [...selectedPathSet][0];
            if (onlyPath === oldPath) {
                selectedPathSet.clear();
                selectedPathSet.add(joinPath(parentDirOf(oldPath), trimmed));
                selectionAnchorPath = [...selectedPathSet][0];
            }
        }
        invalidateEntriesCache();
        await loadList();
    }

    const renderListView = () => {
        const tbody = $('fileList');
        const fragment = document.createDocumentFragment();

        entries.forEach((entry, index) => {
            const name = entryName(entry);
            const itemPath = entryPath(entry);
            const tr = document.createElement('tr');
            tr.dataset.filePath = itemPath;

            const tdName = document.createElement('td');
            const nameSpan = document.createElement('span');
            nameSpan.className = 'name-link';
            nameSpan.appendChild(createFileIcon(name, entry.directory));
            const nameText = document.createElement('span');
            nameText.textContent = name;
            nameSpan.appendChild(nameText);
            if (entry.directory) {
                bindEnterDir(nameSpan, name, {dblclick: true});
            }
            nameSpan.addEventListener('click', (e) => {
                e.stopPropagation();
                applySelectionClick(e, index, itemPath);
            });
            tdName.appendChild(nameSpan);

            const tdTime = document.createElement('td');
            tdTime.textContent = formatTime(entry.modifiedTime || entry.modified_time);

            const tdType = document.createElement('td');
            tdType.textContent = fileTypeLabel(name, entry.directory);

            const tdSize = document.createElement('td');
            tdSize.textContent = entry.directory ? '' : formatSize(entry.size);

            tr.append(tdName, tdTime, tdType, tdSize);
            bindDragSource(tr, itemPath);
            if (entry.directory) {
                bindDropTarget(tr, itemPath);
            } else {
                bindDropTarget(tr, currentPath);
            }
            tr.addEventListener('click', (e) => {
                if (e.target.closest('.name-link')) {
                    return;
                }
                applySelectionClick(e, index, itemPath);
            });
            fragment.appendChild(tr);
        });
        tbody.replaceChildren(fragment);
    };

    const renderIconView = () => {
        const grid = $('iconGrid');
        const fragment = document.createDocumentFragment();

        entries.forEach((entry, index) => {
            const name = entryName(entry);
            const itemPath = entryPath(entry);
            const item = document.createElement('div');
            item.className = 'icon-item';
            item.title = name;
            item.dataset.filePath = itemPath;

            const thumb = document.createElement('div');
            thumb.className = 'icon-item-thumb';
            thumb.appendChild(createFileIcon(name, entry.directory, true));
            item.appendChild(thumb);

            const label = document.createElement('div');
            label.className = 'icon-item-name';
            label.textContent = name;
            item.appendChild(label);

            item.addEventListener('click', (e) => {
                applySelectionClick(e, index, itemPath);
            });

            if (entry.directory) {
                bindEnterDir(thumb, name, {dblclick: true});
                bindEnterDir(label, name, {dblclick: true});
                bindDropTarget(item, itemPath);
            } else {
                bindDropTarget(item, currentPath);
            }

            bindDragSource(item, itemPath);

            fragment.appendChild(item);
        });
        grid.replaceChildren(fragment);
    };

    const renderColumnItem = (entry, colPath, highlightName, index, panelPaths) => {
        const name = entryName(entry);
        const fullPath = entryPath(entry, colPath);
        const item = document.createElement('div');
        item.className = 'column-item';
        item.dataset.filePath = fullPath;
        const isPathHighlight = highlightName && name === highlightName;
        if (isPathHighlight) {
            item.classList.add('is-selected');
        }
        if (selectedPathSet.has(fullPath)) {
            item.classList.add('is-checked');
        }

        item.appendChild(createFileIcon(name, entry.directory));

        const label = document.createElement('span');
        label.className = 'column-item-name';
        label.textContent = name;
        item.appendChild(label);

        if (entry.directory) {
            const chevron = document.createElement('span');
            chevron.className = 'column-item-chevron';
            chevron.setAttribute('aria-hidden', 'true');
            chevron.textContent = '›';
            item.appendChild(chevron);
        }

        item.addEventListener('click', (e) => {
            if (listLoading) {
                return;
            }
            const shift = isRangeSelectModifier(e);
            const multi = isMultiSelectModifier(e);
            applySelectionClick(e, index, fullPath, panelPaths);

            if (!shift && !multi && entry.directory) {
                const targetPath = joinPath(colPath, name);
                if (targetPath !== currentPath) {
                    currentPath = targetPath;
                    loadList().catch(showError);
                }
            }
        });

        bindDragSource(item, fullPath);
        if (entry.directory) {
            bindDropTarget(item, fullPath);
        } else {
            bindDropTarget(item, colPath);
        }

        return item;
    };

    const renderColumnView = async () => {
        const browser = $('columnBrowser');
        const chain = pathChain(currentPath);
        const fragment = document.createDocumentFragment();
        let panelCount = 0;

        for (let i = 0; i < chain.length; i++) {
            const colPath = chain[i];
            const isLastColumn = i === chain.length - 1;
            const colEntries = colPath === currentPath
                ? entries
                : await fetchEntries(colPath);

            if (!colEntries.length && !isLastColumn) {
                continue;
            }

            const highlightName = i + 1 < chain.length
                ? selectedChildName(colPath, chain[i + 1])
                : null;

            const panel = document.createElement('div');
            panel.className = 'column-panel';
            panel.dataset.path = colPath;

            if (!colEntries.length) {
                panel.appendChild(createColumnEmptyContent());
            } else {
                const panelPaths = colEntries.map((entry) => entryPath(entry, colPath));
                colEntries.forEach((entry, index) => {
                    panel.appendChild(renderColumnItem(entry, colPath, highlightName, index, panelPaths));
                });
            }
            fragment.appendChild(panel);
            panelCount += 1;
        }

        browser.replaceChildren(fragment);

        if (panelCount === 0) {
            const panel = document.createElement('div');
            panel.className = 'column-panel';
            panel.dataset.path = currentPath;
            panel.appendChild(createColumnEmptyContent());
            browser.appendChild(panel);
        }

        scrollColumnBrowser(browser);
    };

    async function renderFiles() {
        renderBreadcrumb();
        updateSummary();

        if (viewMode === 'list') {
            renderListView();
        } else if (viewMode === 'icon') {
            renderIconView();
        } else {
            await renderColumnView();
        }
        bindDirectoryDropZones();
        syncSelectionState();
    }

    const navigate = (e) => {
        const link = e.target.closest('[data-path]');
        if (link?.dataset.path) {
            navigateTo(link.dataset.path);
        }
    };

    const enterDir = (name) => {
        if (listLoading) {
            return;
        }
        navigateTo(joinPath(currentPath, name));
    };

    const selectedPaths = () => [...selectedPathSet];

    const uploadSingleFile = (file, itemEl) => new Promise((resolve, reject) => {
        const xhr = new XMLHttpRequest();
        xhr.open('POST', `/api/files/upload?path=${encodeURIComponent(currentPath)}`);
        xhr.withCredentials = true;
        xhr.upload.onprogress = (ev) => {
            if (ev.lengthComputable) {
                itemEl.querySelector('.bar-fill').style.width = `${(ev.loaded / ev.total) * 100}%`;
            }
        };
        xhr.onload = () => {
            if (xhr.status >= 200 && xhr.status < 300) {
                itemEl.querySelector('.bar-fill').style.width = '100%';
                resolve();
                return;
            }
            let msg = '上传失败';
            try {
                const body = JSON.parse(xhr.responseText || '{}');
                msg = extractErrorMessage(body, msg);
            } catch { /* ignore */
            }
            reject(new Error(msg));
        };
        xhr.onerror = () => reject(new Error('网络错误'));
        xhr.onabort = () => reject(new Error('上传已取消'));
        const fd = new FormData();
        fd.append('file', file);
        xhr.send(fd);
    });

    async function uploadFiles(files) {
        if (!files.length) return;
        await ensureWritable('上传文件');
        $('progressPanel').style.display = 'block';
        $('uploadCount').textContent = String(files.length);
        const list = $('progressList');
        list.innerHTML = '';
        let hasError = false;

        for (const file of files) {
            const div = document.createElement('div');
            div.className = 'progress-item';
            div.innerHTML = `${escapeHtml(file.name)}<div class="bar"><div class="bar-fill"></div></div>`;
            list.appendChild(div);

            try {
                await uploadSingleFile(file, div);
            } catch (e) {
                hasError = true;
                div.classList.add('error');
                div.querySelector('.bar').remove();
                div.append(document.createTextNode(` — ${e.message || '上传失败'}`));
            }
        }

        try {
            await loadList();
        } catch (e) {
            if (!hasError) {
                throw e;
            }
        }
        $('fileInput').value = '';
        if (hasError) {
            await showAlert('部分文件上传失败，请查看上传面板中的错误信息', {
                title: '上传未完成',
                type: 'warning'
            });
        }
    }

    const parseFilenameFromDisposition = (header) => {
        if (!header) return null;
        const star = /filename\*=([^']*)''([^;]+)/i.exec(header);
        if (star) {
            try {
                return decodeURIComponent(star[2].trim());
            } catch { /* ignore */
            }
        }
        const quoted = /filename="([^"]*)"/i.exec(header);
        if (quoted?.[1]) return quoted[1];
        const plain = /filename=([^;]+)/i.exec(header);
        if (plain?.[1]) return plain[1].trim();
        return null;
    };

    async function downloadFile(path) {
        const res = await fetch(`/api/files/download?path=${encodeURIComponent(path)}`, {credentials: 'include'});
        if (!res.ok) {
            let msg = '下载失败';
            try {
                msg = extractErrorMessage(await res.json(), msg);
            } catch { /* ignore */
            }
            throw new Error(msg);
        }
        const blob = await res.blob();
        const name = parseFilenameFromDisposition(res.headers.get('Content-Disposition'))
            || path.split('/').pop()
            || 'download';
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = name;
        document.body.appendChild(a);
        a.click();
        a.remove();
        URL.revokeObjectURL(url);
    }

    async function downloadSelected() {
        const paths = selectedPaths();
        const errors = [];
        for (const p of paths) {
            const entry = findEntryByPath(p);
            if (entry?.directory) {
                continue;
            }
            const name = p.split('/').pop();
            try {
                await downloadFile(p);
            } catch (e) {
                errors.push(`${name}: ${e.message || '下载失败'}`);
            }
        }
        if (errors.length) {
            await showAlert(errors.join('\n'), {title: '部分下载失败', type: 'warning'});
        }
    }

    async function mkdir() {
        await ensureWritable('新建文件夹');
        const result = await showPrompt({
            title: '新建文件夹',
            message: '请输入文件夹名称',
            placeholder: '文件夹名称',
            confirmText: '创建',
            cancelText: '取消',
            selectOnOpen: false
        });
        if (result.action !== 'confirm') {
            return;
        }
        const name = (result.value || '').trim();
        if (!name) {
            return;
        }
        await api('/api/files/mkdir', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({path: currentPath, name})
        });
        await loadList();
    }

    const buildDeleteConfirmOptions = (paths) => {
        const hasDirectory = paths.some((p) => findEntryByPath(p)?.directory);
        const count = paths.length;
        if (hasDirectory) {
            return {
                title: count === 1 ? '删除文件夹' : `删除 ${count} 项`,
                message: count === 1
                    ? '文件夹内的所有内容将一并删除，且无法恢复。'
                    : '选中的项目中包含文件夹，将连同内部所有文件一并删除，且无法恢复。',
                type: 'danger',
                danger: true,
                confirmText: '删除',
                cancelText: '取消'
            };
        }
        return {
            title: count === 1 ? '删除文件' : `删除 ${count} 个文件`,
            message: '删除后无法恢复，确定继续吗？',
            type: 'warning',
            danger: true,
            confirmText: '删除',
            cancelText: '取消'
        };
    };

    async function deleteSelected() {
        await ensureWritable('删除文件');
        const paths = selectedPaths();
        if (!paths.length) {
            return;
        }
        const result = await showConfirm(buildDeleteConfirmOptions(paths));
        if (result.action !== 'confirm') {
            return;
        }

        setListLoading(true);
        const errors = [];
        try {
            for (const p of paths) {
                try {
                    await api(`/api/files?path=${encodeURIComponent(p)}`, {method: 'DELETE'});
                } catch (e) {
                    errors.push(`${p.split('/').pop() || p}: ${e.message || '删除失败'}`);
                }
            }
            clearSelection();
            invalidateEntriesCache();
            try {
                await loadList(true);
            } catch (e) {
                errors.push(e.message || '刷新列表失败');
            }
        } finally {
            setListLoading(false);
        }
        if (errors.length) {
            await showAlert(errors.join('\n'), {title: '部分删除失败', type: 'danger'});
        }
    }

    async function bootstrap() {
        initTheme();
        initDialog();
        initViewMode();
        try {
            const status = await refreshAuthStatus();
            scheduleAuthRefresh();

            if (status.loggedIn || !authRequired) {
                await loadList();
            }
        } catch (e) {
            showLogin();
            const errEl = $('loginError');
            errEl.textContent = e.message || '无法连接服务';
            errEl.style.display = 'block';
        }
    }

    document.addEventListener('visibilitychange', () => {
        if (document.visibilityState === 'visible') {
            refreshAuthStatus().catch(() => { /* ignore */
            });
        }
    });

    $('btnTheme').addEventListener('click', toggleTheme);
    $('btnThemeLogin').addEventListener('click', toggleTheme);
    $('btnLogin').addEventListener('click', login);
    $('password').addEventListener('keydown', (e) => {
        if (e.key === 'Enter') login();
    });
    $('btnLogout').addEventListener('click', logout);
    $('btnUpload').addEventListener('click', () => $('fileInput').click());
    $('emptyUploadLink').addEventListener('click', () => $('fileInput').click());
    $('emptyMkdirLink').addEventListener('click', () => mkdir().catch(showError));
    $('fileInput').addEventListener('change', (e) => {
        uploadFiles(e.target.files).catch(showError);
    });
    $('btnDownload').addEventListener('click', () => downloadSelected().catch(showError));
    $('btnMkdir').addEventListener('click', () => mkdir().catch(showError));
    $('btnRename').addEventListener('click', () => renameSelected().catch(showError));
    $('btnDelete').addEventListener('click', () => deleteSelected().catch(showError));
    $('breadcrumb').addEventListener('click', navigate);
    $('btnBack').addEventListener('click', goBack);
    $('btnHome').addEventListener('click', goHome);
    for (const btn of document.querySelectorAll('.view-btn')) {
        btn.addEventListener('click', () => setViewMode(btn.dataset.view));
    }
    $('btnHideProgress').addEventListener('click', () => {
        $('progressPanel').style.display = 'none';
    });

    bootstrap();
})();
