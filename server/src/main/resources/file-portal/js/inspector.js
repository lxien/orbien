(() => {
    'use strict';

    const PREVIEW_UNKNOWN_ICON = '/icon/preview-unknown.svg';
    const TEXT_PREVIEW_MAX_LINES = 500;

    let abortController = null;
    let blobUrls = [];

    const revokeBlobUrls = () => {
        for (const url of blobUrls) {
            URL.revokeObjectURL(url);
        }
        blobUrls = [];
    };

    const cancelPending = () => {
        abortController?.abort();
        abortController = new AbortController();
        revokeBlobUrls();
        return abortController.signal;
    };

    const trackBlobUrl = (url) => {
        blobUrls.push(url);
        return url;
    };

    const formatSize = (n) => {
        if (!n) return '—';
        if (n < 1024) return `${n} B`;
        if (n < 1_048_576) return `${(n / 1024).toFixed(1)} KB`;
        if (n < 1_073_741_824) return `${(n / 1_048_576).toFixed(1)} MB`;
        return `${(n / 1_073_741_824).toFixed(1)} GB`;
    };

    const formatTime = (ms) => (ms ? new Date(Number(ms)).toLocaleString('zh-CN') : '—');

    const kindLabel = (meta) => {
        if (meta.directory) return '文件夹';
        const ext = (meta.name || '').split('.').pop();
        if (ext && ext !== meta.name) return `${ext.toUpperCase()} 文件`;
        return '文件';
    };

    const fetchPreviewMeta = async (path, signal) => {
        const res = await fetch(`/api/files/preview-meta?path=${encodeURIComponent(path)}`, {
            credentials: 'include',
            signal
        });
        if (!res.ok) {
            const data = await res.json().catch(() => ({}));
            throw new Error(data.message || `请求失败: ${res.status}`);
        }
        return res.json();
    };

    const fetchPreviewBlob = async (path, signal) => {
        const res = await fetch(`/api/files/preview?path=${encodeURIComponent(path)}`, {
            credentials: 'include',
            signal
        });
        if (!res.ok) {
            const data = await res.json().catch(() => ({}));
            throw new Error(data.message || `预览失败: ${res.status}`);
        }
        const truncated = res.headers.get('X-Preview-Truncated') === 'true';
        const blob = await res.blob();
        return {blob, truncated};
    };

    const renderLoading = (previewEl) => {
        previewEl.replaceChildren();
        const loading = document.createElement('div');
        loading.className = 'inspector-preview-loading';
        loading.textContent = '加载中…';
        previewEl.appendChild(loading);
    };

    const renderUnknown = (previewEl, meta) => {
        previewEl.replaceChildren();
        const wrap = document.createElement('div');
        wrap.className = 'inspector-preview-unknown';

        const img = document.createElement('img');
        img.className = 'inspector-preview-unknown-icon';
        img.src = PREVIEW_UNKNOWN_ICON;
        img.alt = '';
        wrap.appendChild(img);

        const ext = document.createElement('div');
        ext.className = 'inspector-preview-unknown-ext';
        const dot = (meta.name || '').lastIndexOf('.');
        ext.textContent = dot > 0 ? meta.name.slice(dot + 1).toUpperCase() : 'FILE';
        wrap.appendChild(ext);

        previewEl.appendChild(wrap);
    };

    const renderImage = async (previewEl, path, meta, signal) => {
        const {blob} = await fetchPreviewBlob(path, signal);
        const url = trackBlobUrl(URL.createObjectURL(blob));
        previewEl.replaceChildren();
        const img = document.createElement('img');
        img.className = 'inspector-preview-image';
        img.src = url;
        img.alt = meta.name || '';
        previewEl.appendChild(img);
    };

    const renderText = async (previewEl, path, meta, signal) => {
        const {blob, truncated} = await fetchPreviewBlob(path, signal);
        const text = await blob.text();
        previewEl.replaceChildren();

        const pre = document.createElement('pre');
        pre.className = 'inspector-preview-text';
        const lines = text.split('\n');
        const clipped = lines.length > TEXT_PREVIEW_MAX_LINES;
        const display = clipped ? lines.slice(0, TEXT_PREVIEW_MAX_LINES).join('\n') : text;
        pre.textContent = display;
        previewEl.appendChild(pre);

        if (truncated || clipped) {
            const note = document.createElement('p');
            note.className = 'inspector-preview-truncated';
            note.textContent = truncated
                ? '文件较大，仅显示部分内容'
                : `文本过长，仅显示前 ${TEXT_PREVIEW_MAX_LINES} 行`;
            previewEl.appendChild(note);
        }
    };

    const renderFolderIcon = (previewEl, iconUrl) => {
        previewEl.replaceChildren();
        const wrap = document.createElement('div');
        wrap.className = 'inspector-preview-folder';
        const img = document.createElement('img');
        img.src = iconUrl || '/icon/folder.svg';
        img.alt = '';
        wrap.appendChild(img);
        previewEl.appendChild(wrap);
    };

    const renderDetails = (detailsEl, meta) => {
        detailsEl.replaceChildren();

        const title = document.createElement('h3');
        title.className = 'inspector-details-title';
        title.textContent = meta.name || '—';
        detailsEl.appendChild(title);

        const subtitle = document.createElement('p');
        subtitle.className = 'inspector-details-subtitle';
        subtitle.textContent = meta.directory
            ? '文件夹'
            : `${kindLabel(meta)} · ${formatSize(meta.size)}`;
        detailsEl.appendChild(subtitle);

        const section = document.createElement('div');
        section.className = 'inspector-details-section';

        const heading = document.createElement('div');
        heading.className = 'inspector-details-heading';
        heading.textContent = '信息';
        section.appendChild(heading);

        const rows = [
            ['创建时间', formatTime(meta.createdTime)],
            ['修改时间', formatTime(meta.modifiedTime)],
            ['上次打开', formatTime(meta.lastAccessTime)]
        ];
        if (!meta.directory) {
            rows.push(['大小', formatSize(meta.size)]);
        }

        for (const [label, value] of rows) {
            const row = document.createElement('div');
            row.className = 'inspector-details-row';
            const lbl = document.createElement('span');
            lbl.className = 'inspector-details-label';
            lbl.textContent = label;
            const val = document.createElement('span');
            val.className = 'inspector-details-value';
            val.textContent = value;
            row.append(lbl, val);
            section.appendChild(row);
        }
        detailsEl.appendChild(section);
    };

    const renderPreview = async (previewEl, path, meta, signal) => {
        if (meta.directory) {
            renderFolderIcon(previewEl, '/icon/folder.svg');
            return;
        }
        const kind = (meta.previewKind || '').toLowerCase();
        if (kind === 'image' && meta.previewable) {
            await renderImage(previewEl, path, meta, signal);
            return;
        }
        if (kind === 'text' && meta.previewable) {
            await renderText(previewEl, path, meta, signal);
            return;
        }
        renderUnknown(previewEl, meta);
    };

    const buildInspectorPanel = (mode = 'column') => {
        const panel = document.createElement('div');
        panel.className = `inspector-panel is-${mode}`;

        const preview = document.createElement('div');
        preview.className = 'inspector-preview';
        panel.appendChild(preview);

        const details = document.createElement('div');
        details.className = 'inspector-details';
        panel.appendChild(details);

        return {panel, preview, details};
    };

    const show = async (container, path, entry, options = {}) => {
        if (!container || !path) {
            return;
        }
        const mode = options.mode || (container.closest('.inspector-dialog-panel') ? 'dialog' : 'column');
        const signal = cancelPending();
        const {panel, preview, details} = buildInspectorPanel(mode);
        container.replaceChildren(panel);
        container.classList.remove('hidden');

        const metaFromEntry = {
            path,
            name: entry?.name || entry?.name_ || path.split('/').pop() || '',
            directory: !!entry?.directory,
            size: entry?.size || 0,
            modifiedTime: entry?.modifiedTime || entry?.modified_time,
            createdTime: entry?.createdTime || entry?.created_time,
            lastAccessTime: entry?.lastAccessTime || entry?.last_access_time
        };

        renderLoading(preview);
        renderDetails(details, metaFromEntry);

        try {
            const meta = await fetchPreviewMeta(path, signal);
            if (signal.aborted) {
                return;
            }
            renderDetails(details, meta);
            await renderPreview(preview, path, meta, signal);
        } catch (e) {
            if (signal.aborted || e.name === 'AbortError') {
                return;
            }
            preview.replaceChildren();
            const err = document.createElement('p');
            err.className = 'inspector-preview-error';
            err.textContent = e.message || '预览加载失败';
            preview.appendChild(err);
            if (!options.silent) {
                console.warn('preview failed', e);
            }
        }
    };

    const hide = (container) => {
        cancelPending();
        if (container) {
            container.replaceChildren();
        }
    };

    const openDialog = async (path, entry) => {
        const root = document.getElementById('inspectorDialog');
        const content = document.getElementById('inspectorDialogContent');
        if (!root || !content) {
            return;
        }
        root.classList.remove('hidden');
        root.setAttribute('aria-hidden', 'false');
        document.body.classList.add('inspector-open');
        await show(content, path, entry, {mode: 'dialog'});
    };

    const closeDialog = () => {
        const root = document.getElementById('inspectorDialog');
        const content = document.getElementById('inspectorDialogContent');
        if (root) {
            root.classList.add('hidden');
            root.setAttribute('aria-hidden', 'true');
        }
        document.body.classList.remove('inspector-open');
        hide(content);
    };

    const initDialog = () => {
        const root = document.getElementById('inspectorDialog');
        if (!root) {
            return;
        }
        root.querySelectorAll('[data-inspector-dismiss="true"]').forEach((el) => {
            el.addEventListener('click', closeDialog);
        });
        document.getElementById('inspectorDialogClose')?.addEventListener('click', closeDialog);
        document.addEventListener('keydown', (e) => {
            if (e.key !== 'Escape' || root.classList.contains('hidden')) {
                return;
            }
            const appDialog = document.getElementById('appDialog');
            if (appDialog && !appDialog.classList.contains('hidden')) {
                return;
            }
            closeDialog();
        });
    };

    initDialog();
    window.FileInspector = {show, hide, cancelPending, openDialog, closeDialog};
})();
