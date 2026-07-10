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
    const VIEW_KEY = 'orbien-file-view';
    const AUTH_REFRESH_INTERVAL = 30_000;
    const VIEW_MODES = ['list', 'icon', 'column'];
    const WRITE_CONTROL_IDS = ['btnUpload', 'btnMkdir', 'btnDelete', 'fileInput', 'emptyUploadLink', 'emptyMkdirLink'];
    const THEME_BUTTON_IDS = ['btnTheme', 'btnThemeLogin'];

    let currentPath = '/';
    let entries = [];
    let authRequired = true;
    let canWrite = true;
    let permission = 'read_write';
    let currentUsername = '';
    let authRefreshTimer = null;
    let listLoading = false;
    let viewMode = 'list';
    const entriesCache = new Map();

    const $ = (id) => document.getElementById(id);

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

    const showError = (err) => {
        alert(err.message || String(err));
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

    const applyTheme = (theme) => {
        document.documentElement.setAttribute('data-theme', theme);
        localStorage.setItem(THEME_KEY, theme);
        const label = theme === 'dark' ? '白昼' : '黑夜';
        const title = theme === 'dark' ? '切换到白昼模式' : '切换到黑夜模式';
        for (const id of THEME_BUTTON_IDS) {
            const btn = $(id);
            if (btn) {
                btn.textContent = label;
                btn.title = title;
            }
        }
    };

    const initTheme = () => {
        const saved = localStorage.getItem(THEME_KEY);
        applyTheme(saved === 'dark' ? 'dark' : 'light');
    };

    const toggleTheme = () => {
        const current = document.documentElement.getAttribute('data-theme') || 'light';
        applyTheme(current === 'dark' ? 'light' : 'dark');
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

    const updateWriteControls = () => {
        for (const id of WRITE_CONTROL_IDS) {
            const el = $(id);
            if (el) {
                el.style.display = canWrite ? '' : 'none';
            }
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
        canWrite = data.canWrite === true;
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
        applyViewMode(mode);
        renderFiles().catch(showError);
    };

    const navigateTo = (path) => {
        if (listLoading || path === currentPath) {
            return;
        }
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
            if (!isLast) {
                btn.dataset.path = item.path;
            }
            wrap.appendChild(btn);
            nav.appendChild(wrap);
        });
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

    const createSelectCheckbox = (index, filePath, isDirectory = false) => {
        const checkbox = document.createElement('input');
        checkbox.type = 'checkbox';
        checkbox.className = 'file-select';
        checkbox.dataset.idx = String(index);
        checkbox.dataset.filePath = filePath;
        checkbox.dataset.isDirectory = isDirectory ? '1' : '0';
        checkbox.addEventListener('change', syncSelectionState);
        return checkbox;
    };

    const syncSelectionState = () => {
        const checkboxes = [...document.querySelectorAll('#fileListWrap .file-select')];
        const checked = checkboxes.filter((el) => el.checked);
        $('selectAll').checked = checkboxes.length > 0 && checked.length === checkboxes.length;
        $('selectAll').indeterminate = checked.length > 0 && checked.length < checkboxes.length;

        for (const item of document.querySelectorAll('.icon-item, .column-item')) {
            const checkbox = item.querySelector('.file-select');
            item.classList.toggle('is-selected', !!checkbox?.checked);
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

    const renderListView = () => {
        const tbody = $('fileList');
        const fragment = document.createDocumentFragment();

        entries.forEach((entry, index) => {
            const name = entryName(entry);
            const tr = document.createElement('tr');

            const tdCheck = document.createElement('td');
            tdCheck.appendChild(createSelectCheckbox(index, entryPath(entry), entry.directory));

            const tdName = document.createElement('td');
            const nameSpan = document.createElement('span');
            nameSpan.className = 'name-link';
            nameSpan.appendChild(createFileIcon(name, entry.directory));
            const nameText = document.createElement('span');
            nameText.textContent = name;
            nameSpan.appendChild(nameText);
            if (entry.directory) {
                bindEnterDir(nameSpan, name);
            }
            tdName.appendChild(nameSpan);

            const tdTime = document.createElement('td');
            tdTime.textContent = formatTime(entry.modifiedTime || entry.modified_time);

            const tdType = document.createElement('td');
            tdType.textContent = fileTypeLabel(name, entry.directory);

            const tdSize = document.createElement('td');
            tdSize.textContent = entry.directory ? '' : formatSize(entry.size);

            tr.append(tdCheck, tdName, tdTime, tdType, tdSize);
            fragment.appendChild(tr);
        });
        tbody.replaceChildren(fragment);
    };

    const renderIconView = () => {
        const grid = $('iconGrid');
        const fragment = document.createDocumentFragment();

        entries.forEach((entry, index) => {
            const name = entryName(entry);
            const item = document.createElement('div');
            item.className = 'icon-item';
            item.title = name;

            const checkbox = createSelectCheckbox(index, entryPath(entry), entry.directory);
            checkbox.classList.add('icon-item-check');
            item.appendChild(checkbox);

            const thumb = document.createElement('div');
            thumb.className = 'icon-item-thumb';
            thumb.appendChild(createFileIcon(name, entry.directory, true));
            item.appendChild(thumb);

            const label = document.createElement('div');
            label.className = 'icon-item-name';
            label.textContent = name;
            item.appendChild(label);

            let clickTimer;
            item.addEventListener('click', (e) => {
                if (e.target.closest('input[type=checkbox]')) {
                    return;
                }
                clearTimeout(clickTimer);
                clickTimer = setTimeout(() => {
                    checkbox.checked = !checkbox.checked;
                    syncSelectionState();
                }, 220);
            });

            if (entry.directory) {
                const cancelClick = () => clearTimeout(clickTimer);
                bindEnterDir(thumb, name, {dblclick: true});
                bindEnterDir(label, name, {dblclick: true});
                thumb.addEventListener('dblclick', cancelClick);
                label.addEventListener('dblclick', cancelClick);
            }

            fragment.appendChild(item);
        });
        grid.replaceChildren(fragment);
    };

    const renderColumnItem = (entry, colPath, highlightName) => {
        const name = entryName(entry);
        const fullPath = entryPath(entry, colPath);
        const item = document.createElement('div');
        item.className = 'column-item';
        if (highlightName && name === highlightName) {
            item.classList.add('is-selected');
        }

        const checkbox = createSelectCheckbox(-1, fullPath, entry.directory);
        checkbox.classList.add('column-item-check');
        item.appendChild(checkbox);
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
            if (e.target.closest('input[type=checkbox]')) {
                return;
            }
            const targetPath = joinPath(colPath, name);
            if (entry.directory) {
                if (listLoading) {
                    return;
                }
                currentPath = targetPath;
                loadList().catch(showError);
                return;
            }
            checkbox.checked = !checkbox.checked;
            syncSelectionState();
        });

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
                colEntries.forEach((entry) => {
                    panel.appendChild(renderColumnItem(entry, colPath, highlightName));
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
        $('selectAll').checked = false;
        $('selectAll').indeterminate = false;

        if (viewMode === 'list') {
            renderListView();
        } else if (viewMode === 'icon') {
            renderIconView();
        } else {
            await renderColumnView();
        }
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

    const selectedPaths = () => [...document.querySelectorAll('#fileListWrap .file-select:checked')]
        .map((checkbox) => checkbox.dataset.filePath)
        .filter(Boolean);

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
            alert('部分文件上传失败，请查看上传面板中的错误信息');
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
        for (const checkbox of document.querySelectorAll('#fileListWrap .file-select:checked')) {
            if (checkbox.dataset.isDirectory === '1') {
                continue;
            }
            const p = checkbox.dataset.filePath;
            const name = p.split('/').pop();
            try {
                await downloadFile(p);
            } catch (e) {
                errors.push(`${name}: ${e.message || '下载失败'}`);
            }
        }
        if (errors.length) {
            alert(`部分下载失败：\n${errors.join('\n')}`);
        }
    }

    async function mkdir() {
        await ensureWritable('新建文件夹');
        const name = prompt('文件夹名称');
        if (!name) return;
        await api('/api/files/mkdir', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({path: currentPath, name})
        });
        await loadList();
    }

    async function deleteSelected() {
        await ensureWritable('删除文件');
        const paths = selectedPaths();
        if (!paths.length || !confirm('确认删除选中项？')) return;

        const errors = [];
        for (const p of paths) {
            try {
                await api(`/api/files?path=${encodeURIComponent(p)}`, {method: 'DELETE'});
            } catch (e) {
                errors.push(`${p.split('/').pop() || p}: ${e.message || '删除失败'}`);
            }
        }
        try {
            await loadList();
        } catch (e) {
            errors.push(e.message || '刷新列表失败');
        }
        if (errors.length) {
            alert(`部分删除失败：\n${errors.join('\n')}`);
        }
    }

    const toggleAll = (el) => {
        for (const checkbox of document.querySelectorAll('#fileListWrap .file-select')) {
            checkbox.checked = el.checked;
        }
        syncSelectionState();
    };

    async function bootstrap() {
        initTheme();
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
    $('btnDelete').addEventListener('click', () => deleteSelected().catch(showError));
    $('selectAll').addEventListener('change', (e) => toggleAll(e.target));
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
