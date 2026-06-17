import React from 'react';
import clsx from 'clsx';
import Link from '@docusaurus/Link';
import useBaseUrl from '@docusaurus/useBaseUrl';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import Layout from '@theme/Layout';
import styles from './index.module.css';

const GITHUB = 'https://github.com/xiaoniucode/etp';
const features: Array<{
    id: 'panel' | 'speed' | 'shield' | 'sparkle' | 'globe' | 'stack';
    title: string;
    description: string;
}> = [
    {
        id: 'panel',
        title: '管理面板',
        description: '提供现代化Web UI管理面板，实时监控连接状态、在线设备节点、流量统计与运行指标'
    },
    {
        id: 'speed',
        title: '高性能传输',
        description: '自研高效多路复用流传输协议、全链路零拷贝设计，采用状态机及事件驱动架构，保证系统稳定传输'
    },
    {
        id: 'shield',
        title: '协议与安全',
        description: '支持TCP、HTTP及TCP上层协议代理，提供mTLS双向认证、端到端加密传输和会话级临时隧道机制'
    },
    {
        id: 'sparkle',
        title: '简单易用',
        description: '支持客户端自治运行和集中式远程配置管理同步，无需复杂网络知识即可快速完成服务发布与运维管理'
    },
    {
        id: 'globe',
        title: '跨平台',
        description: '兼容Windows/Linux/macOS/Docker/k8s环境，同时支持嵌入Spring Boot应用，实现灵活集成部署'
    },
    {
        id: 'stack',
        title: '能力矩阵',
        description: '内置压缩传输、负载均衡、访问控制、身份认证、带宽限流、鉴权认证、自定义域名、流量分析等能力'
    },
];


function HeroVisual() {
    const dashboardUrl = useBaseUrl('img/dashboard.png');
    const dashboardBlackUrl = useBaseUrl('img/dashboard_black.png');
    return (
        <div className={styles.heroVisual}>
            <img
                src={dashboardUrl}
                alt="仪表盘预览"
                className={`${styles.heroVisualImg} ${styles.heroVisualImgLight}`}
                loading="lazy"
            />
            <img
                src={dashboardBlackUrl}
                alt="仪表盘预览"
                className={`${styles.heroVisualImg} ${styles.heroVisualImgDark}`}
                loading="lazy"
            />
        </div>
    );
}

function FeatureIcon({id}: { id: (typeof features)[number]['id'] }) {
    const common = {
        className: styles.featureIconSvg,
        viewBox: '0 0 24 24',
        width: 22,
        height: 22,
        'aria-hidden': true as const
    };
    switch (id) {
        case 'panel':
            return (
                <svg {...common} fill="none" stroke="currentColor" strokeWidth="2">
                    <rect x="3" y="3" width="18" height="14" rx="2"/>
                    <path d="M3 9h18M8 17h8" strokeLinecap="round"/>
                </svg>
            );
        case 'speed':
            return (
                <svg {...common} fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z" strokeLinejoin="round"/>
                </svg>
            );
        case 'shield':
            return (
                <svg {...common} fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
                </svg>
            );
        case 'sparkle':
            return (
                <svg {...common} fill="none" stroke="currentColor" strokeWidth="2">
                    <path
                        d="M12 3v3M12 18v3M4.22 4.22l2.12 2.12M17.66 17.66l2.12 2.12M3 12h3M18 12h3M4.22 19.78l2.12-2.12M17.66 6.34l2.12-2.12"/>
                    <circle cx="12" cy="12" r="4"/>
                </svg>
            );
        case 'globe':
            return (
                <svg {...common} fill="none" stroke="currentColor" strokeWidth="2">
                    <circle cx="12" cy="12" r="10"/>
                    <path
                        d="M2 12h20M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"/>
                </svg>
            );
        case 'stack':
            return (
                <svg {...common} fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/>
                </svg>
            );
        default:
            return null;
    }
}

export default function Home() {
    const {siteConfig} = useDocusaurusContext();
    const logoUrl = useBaseUrl('img/logo.png');

    return (
        <Layout title={siteConfig.title} description={siteConfig.tagline}>
            <div className={styles.page}>
                <section className={styles.hero}>
                    <div className={styles.heroInner}>
                        <div className={styles.heroCopy}>
                            <p className={styles.heroEyebrow}>Easy Tunnel Proxy · etp</p>
                            <h1 className={styles.heroTitle}>{siteConfig.tagline}</h1>
                            <p className={styles.heroLead}>
                                面向开发和运维的一站式解决方案。TCP/HTTP多协议代理支持，反向代理、TLS传输加密、压缩、负载均衡、精细限流、访问控制、安全认证、
                                SpringBoot集成、管理面板，具备丰富的场景自定义能力
                            </p>
                            <div className={styles.heroBadge}>
                                <span className={styles.heroBadgeText}>由 Netty 驱动 · 安全可靠 · 操作简单</span>
                            </div>
                            <div className={styles.heroActions}>
                                <Link className={clsx(styles.btn, styles.btnPrimary)} to="/docs/overview">
                                    阅读文档
                                </Link>
                                <Link className={clsx(styles.btn, styles.btnGhost)} href={GITHUB}>
                                    <svg className={styles.githubIcon} viewBox="0 0 24 24" width="18" height="18"
                                         aria-hidden>
                                        <path
                                            fill="currentColor"
                                            d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"/>
                                    </svg>
                                    GitHub
                                </Link>
                            </div>
                        </div>
                        <HeroVisual/>
                    </div>
                </section>


                <section className={styles.featureSection} aria-labelledby="home-features-title">
                    <div className={styles.featureSectionInner}>
                        <ul className={styles.featureGrid}>
                            {features.map((f, i) => (
                                <li key={f.title} className={clsx(styles.featureCard, styles[`featureCard${i + 1}`])}>
                                    <div className={clsx(styles.featureIconWrap, styles[`featureIcon${i + 1}`])}>
                                        <FeatureIcon id={f.id}/>
                                    </div>
                                    <h3 className={styles.featureCardTitle}>{f.title}</h3>
                                    <p className={styles.featureCardDesc}>{f.description}</p>
                                </li>
                            ))}
                        </ul>
                    </div>
                </section>
            </div>
        </Layout>
    );
}
