import {themes as prismThemes} from 'prism-react-renderer';
import type {Config} from '@docusaurus/types';
import type * as Preset from '@docusaurus/preset-classic';

// This runs in Node.js - Don't use client-side code here (browser APIs, JSX...)

const config: Config = {
    title: 'Orbien',
    tagline: 'Orbien 内网穿透平台',
    favicon: 'img/favicon.ico',

    // Future flags, see https://docusaurus.io/docs/api/docusaurus-config#future
    future: {
        v4: true, // Improve compatibility with the upcoming Docusaurus v4
    },

    // Set the production url of your site here
    url: 'https://orbien.github.io',
    baseUrl: '/orbien/',
    organizationName: 'lxien', // Usually your GitHub org/user name.
    projectName: 'orbien', // Usually your repo name.

    onBrokenLinks: 'throw',

    i18n: {
        defaultLocale: 'zh-Hans',
        locales: ['zh-Hans'],
    },
    scripts: [
        {
            src: "https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=ca-pub-9884377286673200",
            async: true,
            crossorigin: 'anonymous',
        }
    ],
    presets: [
        [
            'classic',
            {
                docs: {
                    sidebarPath: './sidebars.ts',
                    // Please change this to your repo.
                    // Remove this to remove the "edit this page" links.
                    editUrl:
                        'https://github.com/facebook/docusaurus/tree/main/packages/create-docusaurus/templates/shared/',
                },
                blog: {
                    showReadingTime: true,
                    feedOptions: {
                        type: ['rss', 'atom'],
                        xslt: true,
                    },
                    // Please change this to your repo.
                    // Remove this to remove the "edit this page" links.
                    editUrl:
                        'https://github.com/facebook/docusaurus/tree/main/packages/create-docusaurus/templates/shared/',
                    // Useful options to enforce blogging best practices
                    onInlineTags: 'warn',
                    onInlineAuthors: 'warn',
                    onUntruncatedBlogPosts: 'warn',
                },
                theme: {
                    customCss: './src/css/custom.css',
                },
            } satisfies Preset.Options,
        ],
    ],

    themeConfig: {
        // Replace with your project's social card
        //image: 'img/docusaurus-social-card.jpg',
        colorMode: {
            respectPrefersColorScheme: true,
        },
        navbar: {
            title: '首页',
            logo: {
                alt: 'orbien',
                src: 'img/logo.png',
            },
            items: [
                {
                    type: 'docSidebar',
                    sidebarId: 'tutorialSidebar',
                    position: 'left',
                    label: '文档',
                },
                {to: '/blog', label: '博客', position: 'right'},
                {
                    href: 'https://github.com/lxien/orbien/issues',
                    label: '问题反馈',
                    position: 'right',
                },
                {
                    href: 'https://github.com/lxien/orbien/discussions',
                    label: 'GitHub社区',
                    position: 'right',
                },
                {
                    href: 'https://github.com/lxien/orbien',
                    label: 'GitHub',
                    position: 'right',
                },
                {
                    type: 'localeDropdown',
                    position: 'right',
                },
            ],
        },
        footer: {
            style: 'dark',
            copyright: `Copyright © ${new Date().getFullYear()} orbien lxien.com`,
        },
        prism: {
            //设置高亮语言
            additionalLanguages: ['powershell', "java", "protobuf", "python", "groovy", "toml"],
            theme: prismThemes.oneDark,//白天主题
            darkTheme: prismThemes.oneDark,//黑夜主题
        },
    } satisfies Preset.ThemeConfig,
};

export default config;
