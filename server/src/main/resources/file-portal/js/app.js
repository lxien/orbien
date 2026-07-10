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

    const PATH_ROOT_LABEL = 'Disk';
    const PATH_DISK_ICON = '/icon/menu/disk.svg';
    const PATH_FOLDER_ICON = '/icon/folder.svg';
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
    const SORT_KEY = 'orbien-file-sort';
    const VALID_SORT_VALUES = new Set([
        '', 'name', 'kind', 'last_opened', 'date_added', 'modified', 'created', 'size'
    ]);
    const AUTH_REFRESH_INTERVAL = 30_000;
    const VIEW_MODES = ['list', 'icon', 'column'];
    const THEME_BUTTON_IDS = ['btnTheme', 'btnThemeLogin'];

    let currentPath = '/';
    let entries = [];
    let authRequired = true;
    let canWrite = false;
    let canUpload = false;
    let canDelete = false;
    let canMkdir = false;
    let canMove = false;
    let canRename = false;
    let permission = 'read_write';
    let currentUsername = '';
    let authRefreshTimer = null;
    let listLoading = false;
    let viewMode = 'list';
    let currentSort = '';
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
        if (currentPath && currentPath !== '/') {
            validPaths.add(currentPath);
        }
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

    const formatUploadDuration = (ms) => {
        if (!Number.isFinite(ms) || ms < 0) {
            return '';
        }
        if (ms < 60_000) {
            const sec = ms / 1000;
            return sec < 10 ? `${sec.toFixed(1)} 秒` : `${Math.round(sec)} 秒`;
        }
        const min = Math.floor(ms / 60_000);
        const sec = Math.round((ms % 60_000) / 1000);
        return sec > 0 ? `${min} 分 ${sec} 秒` : `${min} 分`;
    };

    const permissionLabel = () => {
        if (permission === 'read_write') {
            return canWrite ? '读写' : '浏览';
        }
        return '只读';
    };

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

    const entriesCacheKey = (path, sort) => `${path}\0${sort || ''}`;

    const invalidateEntriesCache = () => entriesCache.clear();

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
        if (!authRequired) {
            badge.classList.add('hidden');
            return;
        }
        badge.classList.remove('hidden');
        badge.textContent = permissionLabel();
        badge.className = `permission-badge ${canWrite ? 'write' : 'read'}`;
    };

    const setActionVisible = (id, visible) => {
        const el = $(id);
        if (el) {
            el.style.display = visible ? '' : 'none';
        }
    };

    const applyCapabilities = (data) => {
        canUpload = data.canUpload === true;
        canDelete = data.canDelete === true;
        canMkdir = data.canMkdir === true;
        canMove = data.canMove === true;
        canRename = data.canRename === true;
        canWrite = data.canWrite === true;
        updateActionControls();
        updatePermissionBadge();
    };

    const updateActionControls = () => {
        setActionVisible('btnUpload', canUpload);
        setActionVisible('btnDownload', true);
        setActionVisible('btnMkdir', canMkdir);
        setActionVisible('btnRename', canRename);
        setActionVisible('btnDelete', canDelete);
        setActionVisible('fileInput', canUpload);
        setActionVisible('emptyUploadLink', canUpload);
        setActionVisible('emptyMkdirLink', canMkdir);

        const emptyDivider = document.querySelector('#emptyState .empty-divider');
        if (emptyDivider) {
            emptyDivider.style.display = canUpload && canMkdir ? '' : 'none';
        }
        const emptyActions = document.querySelector('#emptyState .empty-actions');
        if (emptyActions) {
            emptyActions.style.display = canUpload || canMkdir ? '' : 'none';
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
        updateActionControls();
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
            applyCapabilities(data);
            permission = data.permission || (data.canWrite ? 'read_write' : 'read');
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
        canUpload = false;
        canDelete = false;
        canMkdir = false;
        canMove = false;
        canRename = false;
        permission = 'read';
        showLogin();
    }

    async function ensureUploadable() {
        await refreshAuthStatus();
        return canUpload;
    }

    async function ensureDeletable() {
        await refreshAuthStatus();
        return canDelete;
    }

    async function ensureMkdirable() {
        await refreshAuthStatus();
        return canMkdir;
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
        /* 路径导航由面包屑承担，不再显示返回按钮 */
    };

    const buildPathItems = () => {
        const parts = currentPath === '/' ? [] : currentPath.split('/').filter(Boolean);
        const items = [{label: PATH_ROOT_LABEL, path: '/', icon: PATH_DISK_ICON}];
        let acc = '';
        for (const part of parts) {
            acc += `/${part}`;
            items.push({label: part, path: acc, icon: PATH_FOLDER_ICON});
        }
        return items;
    };

    const createPathSegment = (item, isLast) => {
        const wrap = document.createElement('span');
        wrap.className = 'path-segment-wrap';

        const btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'path-segment';
        if (isLast) {
            btn.classList.add('is-current');
        }
        btn.title = item.label;
        btn.dataset.path = item.path;

        const icon = document.createElement('img');
        icon.className = 'path-segment-icon';
        icon.src = item.icon;
        icon.alt = '';
        btn.appendChild(icon);

        const label = document.createElement('span');
        label.className = 'path-segment-label';
        label.textContent = item.label;
        btn.appendChild(label);

        wrap.appendChild(btn);
        return wrap;
    };

    const renderPathBar = (nav, {chevrons = false} = {}) => {
        if (!nav) {
            return;
        }
        nav.replaceChildren();
        const items = buildPathItems();
        items.forEach((item, index) => {
            if (chevrons && index > 0) {
                const sep = document.createElement('span');
                sep.className = 'path-segment-chevron';
                sep.textContent = '›';
                sep.setAttribute('aria-hidden', 'true');
                nav.appendChild(sep);
            }
            nav.appendChild(createPathSegment(item, index === items.length - 1));
        });
    };

    const renderBreadcrumb = () => {
        renderPathBar($('breadcrumb'), {chevrons: true});
        renderPathBar($('breadcrumbBottom'), {chevrons: true});
        bindBreadcrumbDropTargets();
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

    const normalizeSort = (sort) => (VALID_SORT_VALUES.has(sort) ? sort : '');

    const applySortSelection = () => {
        for (const option of document.querySelectorAll('.sort-option')) {
            const active = option.dataset.sort === currentSort;
            option.setAttribute('aria-checked', active ? 'true' : 'false');
        }
    };

    const closeSortDropdown = () => {
        const dropdown = $('sortDropdown');
        const btn = $('btnSort');
        if (!dropdown || !btn) {
            return;
        }
        dropdown.classList.add('hidden');
        btn.setAttribute('aria-expanded', 'false');
    };

    const toggleSortDropdown = () => {
        const dropdown = $('sortDropdown');
        const btn = $('btnSort');
        if (!dropdown || !btn) {
            return;
        }
        const open = dropdown.classList.contains('hidden');
        if (open) {
            dropdown.classList.remove('hidden');
            btn.setAttribute('aria-expanded', 'true');
        } else {
            closeSortDropdown();
        }
    };

    const setSort = (sort) => {
        const next = normalizeSort(sort);
        if (next === currentSort) {
            closeSortDropdown();
            return;
        }
        currentSort = next;
        localStorage.setItem(SORT_KEY, currentSort);
        applySortSelection();
        closeSortDropdown();
        invalidateEntriesCache();
        loadList().catch(showError);
    };

    const initSortMenu = () => {
        currentSort = normalizeSort(localStorage.getItem(SORT_KEY) || '');
        applySortSelection();

        $('btnSort')?.addEventListener('click', (e) => {
            e.stopPropagation();
            toggleSortDropdown();
        });

        for (const option of document.querySelectorAll('.sort-option')) {
            option.addEventListener('click', () => setSort(option.dataset.sort || ''));
        }

        document.addEventListener('click', (e) => {
            const wrap = document.querySelector('.sort-menu-wrap');
            if (wrap && !wrap.contains(e.target)) {
                closeSortDropdown();
            }
        });

        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape') {
                closeSortDropdown();
            }
        });
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
        const key = entriesCacheKey(path, currentSort);
        if (useCache && entriesCache.has(key)) {
            return entriesCache.get(key);
        }
        let url = `/api/files/list?path=${encodeURIComponent(path)}`;
        if (currentSort) {
            url += `&sort=${encodeURIComponent(currentSort)}`;
        }
        const data = await api(url);
        const list = data.entries || data.entriesList || [];
        entriesCache.set(key, list);
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

    async function refreshListQuiet() {
        invalidateEntriesCache();
        entries = await fetchEntries(currentPath, false);
        pruneSelection();
        await renderFiles();
    }

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
        text.textContent = '此文件夹为空';
        wrap.appendChild(text);

        if (canUpload || canMkdir) {
            const actions = document.createElement('div');
            actions.className = 'column-panel-empty-actions';

            if (canUpload) {
                const uploadBtn = document.createElement('button');
                uploadBtn.type = 'button';
                uploadBtn.className = 'empty-action';
                uploadBtn.textContent = '上传文件';
                uploadBtn.addEventListener('click', () => $('fileInput').click());
                actions.appendChild(uploadBtn);
            }

            if (canUpload && canMkdir) {
                const divider = document.createElement('span');
                divider.className = 'empty-divider';
                divider.setAttribute('aria-hidden', 'true');
                actions.appendChild(divider);
            }

            if (canMkdir) {
                const mkdirBtn = document.createElement('button');
                mkdirBtn.type = 'button';
                mkdirBtn.className = 'empty-action';
                mkdirBtn.textContent = '新建文件夹';
                mkdirBtn.addEventListener('click', () => mkdir().catch(showError));
                actions.appendChild(mkdirBtn);
            }

            wrap.appendChild(actions);
        }
        return wrap;
    };

    const scrollColumnBrowser = (browser) => {
        requestAnimationFrame(() => {
            const selected = browser.querySelector('.column-item.is-path-active, .column-item.is-path');
            if (selected) {
                selected.scrollIntoView({block: 'nearest', inline: 'nearest'});
            }
            browser.scrollLeft = Math.max(0, browser.scrollWidth - browser.clientWidth);
        });
    };

    const entryPath = (entry, basePath = currentPath) => joinPath(basePath, entryName(entry));

    const findEntryByPath = (path) => {
        if (!path) {
            return undefined;
        }
        if (path === currentPath) {
            const name = path === '/' ? '' : path.split('/').filter(Boolean).pop();
            return {directory: true, name: name || 'Disk'};
        }
        const parent = parentDirOf(path);
        const name = path.split('/').filter(Boolean).pop();
        const list = parent === currentPath
            ? entries
            : entriesCache.get(entriesCacheKey(parent, currentSort));
        return list?.find((entry) => entryName(entry) === name);
    };

    const syncSelectionState = () => {
        const fileSelectedInCurrent = viewMode === 'column' && hasFileSelectionInCurrentPanel(entries);
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
            let checked = !!path && selectedPathSet.has(path);
            if (checked && fileSelectedInCurrent && item.classList.contains('is-path')) {
                checked = false;
            }
            item.classList.toggle('is-checked', checked);
        }
    };

    const MARQUEE_DRAG_THRESHOLD = 4;
    const MARQUEE_AUTO_SCROLL_EDGE = 28;
    const MARQUEE_AUTO_SCROLL_MAX = 18;

    let marqueeState = null;

    const rectsIntersect = (a, b) => !(
        a.right < b.left
        || a.left > b.right
        || a.bottom < b.top
        || a.top > b.bottom
    );

    const normalizeClientRect = (x1, y1, x2, y2) => {
        const left = Math.min(x1, x2);
        const top = Math.min(y1, y2);
        return {
            left,
            top,
            right: Math.max(x1, x2),
            bottom: Math.max(y1, y2),
            width: Math.abs(x2 - x1),
            height: Math.abs(y2 - y1)
        };
    };

    const isMarqueeBlockedTarget = (target) => {
        if (!target?.closest) {
            return true;
        }
        if (target.closest('#fileList tr[data-file-path], .icon-item[data-file-path], .column-item[data-file-path]')) {
            return true;
        }
        if (target.closest('button, input, a, label, .name-link, thead, .empty-action')) {
            return true;
        }
        return !target.closest('#fileListWrap');
    };

    const getMarqueeItems = (panel) => {
        if (viewMode === 'column' && panel) {
            return [...panel.querySelectorAll('.column-item[data-file-path]')];
        }
        if (viewMode === 'icon') {
            return [...document.querySelectorAll('#iconGrid .icon-item[data-file-path]')];
        }
        return [...document.querySelectorAll('#fileList tr[data-file-path]')];
    };

    const getMarqueeScrollContainers = (panel) => {
        const containers = [$('fileListWrap')];
        if (viewMode === 'column') {
            const browser = $('columnBrowser');
            if (browser) {
                containers.push(browser);
            }
            if (panel) {
                containers.push(panel);
            }
        }
        return containers.filter(Boolean);
    };

    const resolveMarqueePanel = (clientX, clientY) => {
        if (viewMode !== 'column') {
            return null;
        }
        const panels = [...document.querySelectorAll('#columnBrowser .column-panel')];
        for (const panel of panels) {
            const rect = panel.getBoundingClientRect();
            if (clientX >= rect.left && clientX <= rect.right
                && clientY >= rect.top && clientY <= rect.bottom) {
                return panel;
            }
        }
        return null;
    };

    const refreshMarqueeAt = (clientX, clientY) => {
        if (!marqueeState?.active) {
            return;
        }
        marqueeState.currentX = clientX;
        marqueeState.currentY = clientY;
        const rect = normalizeClientRect(marqueeState.startX, marqueeState.startY, clientX, clientY);
        updateMarqueeVisual(rect);
        applyMarqueeSelection(rect);
        autoScrollMarquee(clientX, clientY);
    };

    const tickMarqueeAutoScroll = () => {
        if (!marqueeState?.active) {
            marqueeState.rafId = null;
            return;
        }
        refreshMarqueeAt(marqueeState.currentX, marqueeState.currentY);
        marqueeState.rafId = requestAnimationFrame(tickMarqueeAutoScroll);
    };

    const startMarqueeAutoScrollLoop = () => {
        if (!marqueeState || marqueeState.rafId !== null) {
            return;
        }
        marqueeState.rafId = requestAnimationFrame(tickMarqueeAutoScroll);
    };

    const stopMarqueeAutoScrollLoop = () => {
        if (marqueeState?.rafId != null) {
            cancelAnimationFrame(marqueeState.rafId);
            marqueeState.rafId = null;
        }
    };

    const applyMarqueeSelection = (rect) => {
        if (!marqueeState) {
            return;
        }
        const items = getMarqueeItems(marqueeState.panel);
        const next = marqueeState.additive ? new Set(marqueeState.baseSelection) : new Set();
        for (const item of items) {
            const path = item.dataset.filePath;
            if (!path) {
                continue;
            }
            if (rectsIntersect(rect, item.getBoundingClientRect())) {
                next.add(path);
            }
        }
        selectedPathSet.clear();
        for (const path of next) {
            selectedPathSet.add(path);
        }
        if (next.size > 0) {
            selectionAnchorPath = [...next][0];
        }
        syncSelectionState();
    };

    const updateMarqueeVisual = (rect) => {
        const marquee = $('selectionMarquee');
        if (!marquee) {
            return;
        }
        marquee.classList.remove('hidden');
        marquee.style.left = `${rect.left}px`;
        marquee.style.top = `${rect.top}px`;
        marquee.style.width = `${rect.width}px`;
        marquee.style.height = `${rect.height}px`;
    };

    const hideMarqueeVisual = () => {
        const marquee = $('selectionMarquee');
        if (marquee) {
            marquee.classList.add('hidden');
            marquee.style.width = '0';
            marquee.style.height = '0';
        }
    };

    const autoScrollMarquee = (clientX, clientY) => {
        if (!marqueeState?.active) {
            return;
        }
        for (const container of getMarqueeScrollContainers(marqueeState.panel)) {
            const rect = container.getBoundingClientRect();
            let deltaX = 0;
            let deltaY = 0;
            if (clientX < rect.left + MARQUEE_AUTO_SCROLL_EDGE) {
                deltaX = -Math.min(MARQUEE_AUTO_SCROLL_MAX, rect.left + MARQUEE_AUTO_SCROLL_EDGE - clientX);
            } else if (clientX > rect.right - MARQUEE_AUTO_SCROLL_EDGE) {
                deltaX = Math.min(MARQUEE_AUTO_SCROLL_MAX, clientX - (rect.right - MARQUEE_AUTO_SCROLL_EDGE));
            }
            if (clientY < rect.top + MARQUEE_AUTO_SCROLL_EDGE) {
                deltaY = -Math.min(MARQUEE_AUTO_SCROLL_MAX, rect.top + MARQUEE_AUTO_SCROLL_EDGE - clientY);
            } else if (clientY > rect.bottom - MARQUEE_AUTO_SCROLL_EDGE) {
                deltaY = Math.min(MARQUEE_AUTO_SCROLL_MAX, clientY - (rect.bottom - MARQUEE_AUTO_SCROLL_EDGE));
            }
            if (deltaX !== 0) {
                container.scrollLeft += deltaX;
            }
            if (deltaY !== 0) {
                container.scrollTop += deltaY;
            }
        }
    };

    const finishMarqueeSelection = () => {
        stopMarqueeAutoScrollLoop();
        document.body.classList.remove('is-marquee-selecting');
        hideMarqueeVisual();
        marqueeState = null;
    };

    const onMarqueeMouseMove = (e) => {
        if (!marqueeState || listLoading) {
            return;
        }
        const dx = Math.abs(e.clientX - marqueeState.startX);
        const dy = Math.abs(e.clientY - marqueeState.startY);
        if (!marqueeState.active) {
            if (dx < MARQUEE_DRAG_THRESHOLD && dy < MARQUEE_DRAG_THRESHOLD) {
                return;
            }
            marqueeState.active = true;
            document.body.classList.add('is-marquee-selecting');
            startMarqueeAutoScrollLoop();
        }
        marqueeState.moved = true;
        refreshMarqueeAt(e.clientX, e.clientY);
    };

    const onMarqueeMouseUp = (e) => {
        if (!marqueeState) {
            return;
        }
        if (!marqueeState.active && !marqueeState.moved) {
            if (!isMultiSelectModifier(e) && !isRangeSelectModifier(e)) {
                clearSelection();
                syncSelectionState();
            }
        }
        finishMarqueeSelection();
        document.removeEventListener('mousemove', onMarqueeMouseMove);
        document.removeEventListener('mouseup', onMarqueeMouseUp);
    };

    const initMarqueeSelection = () => {
        const wrap = $('fileListWrap');
        if (!wrap) {
            return;
        }
        wrap.addEventListener('mousedown', (e) => {
            if (e.button !== 0 || listLoading || marqueeState) {
                return;
            }
            if (isMarqueeBlockedTarget(e.target)) {
                return;
            }
            const panel = resolveMarqueePanel(e.clientX, e.clientY);
            if (viewMode === 'column' && !panel) {
                return;
            }
            e.preventDefault();
            marqueeState = {
                startX: e.clientX,
                startY: e.clientY,
                currentX: e.clientX,
                currentY: e.clientY,
                active: false,
                moved: false,
                rafId: null,
                additive: isMultiSelectModifier(e) || isRangeSelectModifier(e),
                baseSelection: new Set(selectedPathSet),
                panel
            };
            document.addEventListener('mousemove', onMarqueeMouseMove);
            document.addEventListener('mouseup', onMarqueeMouseUp);
        });
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

    const hasFileSelectionInCurrentPanel = (panelEntries) => {
        if (!selectedPathSet.size || !panelEntries?.length) {
            return false;
        }
        for (const selectedPath of selectedPathSet) {
            if (parentDirOf(selectedPath) !== currentPath) {
                continue;
            }
            const baseName = selectedPath.split('/').filter(Boolean).pop();
            const match = panelEntries.find((e) => entryName(e) === baseName);
            if (match && !match.directory) {
                return true;
            }
        }
        return false;
    };

    const resolveColumnPathHighlightMode = (panelIndex, chainLength, fileSelectedInCurrent) => {
        if (fileSelectedInCurrent) {
            return 'parent';
        }
        return panelIndex === chainLength - 2 ? 'active' : 'parent';
    };

    const shouldApplyColumnPathHighlight = (colPath, highlightName, itemName) => {
        if (!highlightName || itemName !== highlightName) {
            return false;
        }
        const folderPath = joinPath(colPath, highlightName);
        for (const selectedPath of selectedPathSet) {
            if (parentDirOf(selectedPath) !== colPath) {
                continue;
            }
            if (selectedPath === folderPath) {
                continue;
            }
            return false;
        }
        return true;
    };

    const syncColumnPathForSelection = (colPath, entry) => {
        if (viewMode !== 'column' || entry.directory || colPath === currentPath) {
            return false;
        }
        currentPath = colPath;
        return true;
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
        bindDropTarget($('breadcrumb'), currentPath);
        bindDropTarget($('breadcrumbBottom'), currentPath);
        for (const panel of document.querySelectorAll('.column-panel')) {
            bindDropTarget(panel, panel.dataset.path);
        }
    };

    const bindBreadcrumbDropTargets = () => {
        for (const btn of document.querySelectorAll('.path-segment[data-path]')) {
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
        return canRename;
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
        if (!(await ensureRenamable())) {
            return;
        }
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
        const newPath = joinPath(parentDirOf(oldPath), trimmed);
        if (currentPath === oldPath || currentPath.startsWith(`${oldPath}/`)) {
            currentPath = currentPath === oldPath
                ? newPath
                : `${newPath}${currentPath.slice(oldPath.length)}`;
        }
        if (selectedPathSet.has(oldPath)) {
            selectedPathSet.delete(oldPath);
            selectedPathSet.add(newPath);
            selectionAnchorPath = newPath;
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
                applySelectionClick(e, index, itemPath);
            });
            if (entry.directory) {
                tr.dataset.isDir = 'true';
                bindEnterDir(tr, name, {dblclick: true});
            }
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

    const renderColumnItem = (entry, colPath, highlightName, index, panelPaths, pathHighlightMode) => {
        const name = entryName(entry);
        const fullPath = entryPath(entry, colPath);
        const item = document.createElement('div');
        item.className = 'column-item';
        item.dataset.filePath = fullPath;
        const isPathHighlight = highlightName
            && name === highlightName
            && shouldApplyColumnPathHighlight(colPath, highlightName, name);
        if (isPathHighlight) {
            item.classList.add(pathHighlightMode === 'active' ? 'is-path-active' : 'is-path');
        }
        const isPrimarySelection = selectedPathSet.has(fullPath);
        const suppressPathCheck = isPathHighlight && pathHighlightMode === 'parent';
        if (isPrimarySelection && !suppressPathCheck) {
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
            const pathTrimmed = syncColumnPathForSelection(colPath, entry);
            applySelectionClick(e, index, fullPath, panelPaths);

            if (!shift && !multi && entry.directory) {
                const targetPath = joinPath(colPath, name);
                if (targetPath !== currentPath) {
                    currentPath = targetPath;
                    loadList().catch(showError);
                    return;
                }
            }
            if (pathTrimmed) {
                loadList().catch(showError);
                return;
            }
            if (!entry.directory && colPath === currentPath) {
                renderColumnView().catch(showError);
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

        const fileSelectedInCurrent = hasFileSelectionInCurrentPanel(entries);

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
            const pathHighlightMode = highlightName
                ? resolveColumnPathHighlightMode(i, chain.length, fileSelectedInCurrent)
                : 'parent';

            const panel = document.createElement('div');
            panel.className = 'column-panel';
            panel.dataset.path = colPath;

            if (!colEntries.length) {
                panel.appendChild(createColumnEmptyContent());
            } else {
                const panelPaths = colEntries.map((entry) => entryPath(entry, colPath));
                colEntries.forEach((entry, index) => {
                    panel.appendChild(renderColumnItem(
                        entry, colPath, highlightName, index, panelPaths, pathHighlightMode));
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

    const UPLOAD_CONCURRENCY = 3;

    const createUploadProgressItem = (file) => {
        const div = document.createElement('div');
        div.className = 'progress-item uploading';

        const head = document.createElement('div');
        head.className = 'progress-item-head';

        const name = document.createElement('span');
        name.className = 'progress-item-name';
        name.title = file.name;
        name.textContent = file.name;

        const duration = document.createElement('span');
        duration.className = 'progress-item-duration';
        duration.textContent = '0.0 秒';

        head.append(name, duration);

        const bar = document.createElement('div');
        bar.className = 'bar';
        const barFill = document.createElement('div');
        barFill.className = 'bar-fill';
        bar.appendChild(barFill);

        div.append(head, bar);
        return div;
    };

    const uploadSingleFile = (file, itemEl) => new Promise((resolve, reject) => {
        const startTime = performance.now();
        const durationEl = itemEl.querySelector('.progress-item-duration');
        let done = false;

        const setDurationText = (elapsed, finished = false) => {
            if (!durationEl) {
                return;
            }
            const text = formatUploadDuration(elapsed);
            durationEl.textContent = finished ? `用时 ${text}` : text;
        };

        const tickDuration = () => {
            if (!done) {
                setDurationText(performance.now() - startTime);
            }
        };

        tickDuration();
        const timer = setInterval(tickDuration, 100);

        const finish = (callback) => {
            done = true;
            clearInterval(timer);
            setDurationText(performance.now() - startTime, true);
            callback();
        };

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
                finish(() => resolve(performance.now() - startTime));
                return;
            }
            let msg = '上传失败';
            try {
                const body = JSON.parse(xhr.responseText || '{}');
                msg = extractErrorMessage(body, msg);
            } catch { /* ignore */
            }
            finish(() => reject(new Error(msg)));
        };
        xhr.onerror = () => finish(() => reject(new Error('网络错误')));
        xhr.onabort = () => finish(() => reject(new Error('上传已取消')));
        const fd = new FormData();
        fd.append('file', file);
        xhr.send(fd);
    });

    async function uploadFiles(files) {
        if (!files.length || !(await ensureUploadable())) {
            return;
        }

        const fileArr = [...files];
        $('progressPanel').style.display = 'block';
        $('uploadCount').textContent = String(fileArr.length);
        const list = $('progressList');
        list.innerHTML = '';
        let hasError = false;
        let nextIndex = 0;

        const uploadOne = async (file) => {
            const itemEl = createUploadProgressItem(file);
            list.appendChild(itemEl);

            try {
                await uploadSingleFile(file, itemEl);
                itemEl.classList.remove('uploading');
                itemEl.classList.add('done');
                try {
                    await refreshListQuiet();
                } catch { /* 单项刷新失败时，结束后会再同步一次 */
                }
            } catch (e) {
                hasError = true;
                itemEl.classList.remove('uploading');
                itemEl.classList.add('error');
                itemEl.querySelector('.bar')?.remove();
                const err = document.createElement('span');
                err.className = 'progress-item-error';
                err.textContent = ` — ${e.message || '上传失败'}`;
                itemEl.querySelector('.progress-item-head')?.appendChild(err);
            }
        };

        const workers = Array.from(
            {length: Math.min(UPLOAD_CONCURRENCY, fileArr.length)},
            async () => {
                while (nextIndex < fileArr.length) {
                    const file = fileArr[nextIndex++];
                    await uploadOne(file);
                }
            }
        );
        await Promise.all(workers);

        try {
            await refreshListQuiet();
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
        if (!(await ensureMkdirable())) {
            return;
        }
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
        if (!(await ensureDeletable())) {
            return;
        }
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
        initMarqueeSelection();
        initViewMode();
        initSortMenu();
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
    $('breadcrumbBottom').addEventListener('click', navigate);
    $('btnHome').addEventListener('click', goHome);
    for (const btn of document.querySelectorAll('.view-btn')) {
        btn.addEventListener('click', () => setViewMode(btn.dataset.view));
    }
    $('btnHideProgress').addEventListener('click', () => {
        $('progressPanel').style.display = 'none';
    });

    bootstrap();
})();
