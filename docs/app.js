// Relay Documentation & Showcase Engine

document.addEventListener('DOMContentLoaded', () => {
    initTabs();
    initCopyButtons();
    initNavigation();
    initSearch();
    initMobileMenu();
    refreshIcons();
});

function refreshIcons() {
    if (window.lucide) {
        lucide.createIcons();
    }
}

// Tab Switcher (IDE and Dependency tabs)
function initTabs() {
    const tabButtons = document.querySelectorAll('[data-tab-group]');
    tabButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            const group = btn.getAttribute('data-tab-group');
            const target = btn.getAttribute('data-tab-target');

            // Deactivate siblings in this group
            document.querySelectorAll(`[data-tab-group="${group}"]`).forEach(b => {
                b.classList.remove('bg-cyan-500/15', 'text-cyan-400', 'border-cyan-500/30');
                b.classList.add('text-slate-400', 'border-transparent');
            });

            // Activate clicked button
            btn.classList.add('bg-cyan-500/15', 'text-cyan-400', 'border-cyan-500/30');
            btn.classList.remove('text-slate-400', 'border-transparent');

            // Show corresponding panel
            document.querySelectorAll(`[data-tab-panel="${group}"]`).forEach(panel => {
                if (panel.getAttribute('data-panel-id') === target) {
                    panel.classList.remove('hidden');
                } else {
                    panel.classList.add('hidden');
                }
            });

            if (window.Prism) {
                Prism.highlightAll();
            }
            refreshIcons();
        });
    });
}

// Copy Code Snippets
function initCopyButtons() {
    document.querySelectorAll('.copy-btn').forEach(button => {
        button.addEventListener('click', () => {
            const targetId = button.getAttribute('data-clipboard-target');
            let textToCopy = '';

            if (targetId) {
                const targetElement = document.getElementById(targetId);
                textToCopy = targetElement ? targetElement.innerText : '';
            } else {
                const pre = button.closest('.ide-window')?.querySelector('code');
                textToCopy = pre ? pre.innerText : '';
            }

            if (textToCopy) {
                navigator.clipboard.writeText(textToCopy.trim()).then(() => {
                    const originalHtml = button.innerHTML;
                    button.innerHTML = `
                        <svg class="w-3.5 h-3.5 text-emerald-400 inline" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path>
                        </svg>
                        <span class="text-xs text-emerald-400 font-medium ml-1">Kopyalandı</span>
                    `;
                    setTimeout(() => {
                        button.innerHTML = originalHtml;
                    }, 2000);
                });
            }
        });
    });
}

// Sidebar Navigation & Hash routing
function initNavigation() {
    const sidebarLinks = document.querySelectorAll('.sidebar-link');
    const articles = document.querySelectorAll('.wiki-article');
    const breadcrumbTitle = document.getElementById('doc-breadcrumb-title');

    function setActiveDoc(hash) {
        if (!hash || hash === '#' || hash === '#home') {
            document.getElementById('landing-view').classList.remove('hidden');
            document.getElementById('docs-view').classList.add('hidden');
            window.scrollTo({ top: 0, behavior: 'smooth' });
            refreshIcons();
            return;
        }

        // Show docs view
        document.getElementById('landing-view').classList.add('hidden');
        document.getElementById('docs-view').classList.remove('hidden');

        const targetId = hash.replace('#', '');
        let currentTitle = 'Dokümantasyon';

        articles.forEach(article => {
            if (article.id === targetId) {
                article.classList.remove('hidden');
                const h1 = article.querySelector('h1');
                if (h1) currentTitle = h1.innerText;
            } else {
                article.classList.add('hidden');
            }
        });

        if (breadcrumbTitle) {
            breadcrumbTitle.innerText = currentTitle;
        }

        // Update active sidebar link
        sidebarLinks.forEach(link => {
            if (link.getAttribute('href') === hash) {
                link.classList.add('active');
            } else {
                link.classList.remove('active');
            }
        });

        window.scrollTo({ top: 0, behavior: 'smooth' });
        if (window.Prism) {
            Prism.highlightAll();
        }
        refreshIcons();
    }

    // Attach click handlers to all anchor links pointing to hash
    document.querySelectorAll('a[href^="#"]').forEach(link => {
        link.addEventListener('click', (e) => {
            const hash = link.getAttribute('href');
            if (hash.startsWith('#')) {
                history.pushState(null, null, hash);
                setActiveDoc(hash);
            }
        });
    });

    window.addEventListener('popstate', () => {
        setActiveDoc(window.location.hash);
    });

    // Check initial hash
    if (window.location.hash && window.location.hash !== '#home') {
        setActiveDoc(window.location.hash);
    }
}

// Docs Quick Search
function initSearch() {
    const searchInput = document.getElementById('docs-search');
    if (!searchInput) return;

    searchInput.addEventListener('input', (e) => {
        const query = e.target.value.toLowerCase().trim();
        const sidebarLinks = document.querySelectorAll('.sidebar-link');

        sidebarLinks.forEach(link => {
            const title = link.innerText.toLowerCase();
            if (title.includes(query) || query === '') {
                link.classList.remove('hidden');
            } else {
                link.classList.add('hidden');
            }
        });
    });
}

// Mobile Menu
function initMobileMenu() {
    const mobileBtn = document.getElementById('mobile-menu-btn');
    const mobileMenu = document.getElementById('mobile-menu');

    if (mobileBtn && mobileMenu) {
        mobileBtn.addEventListener('click', () => {
            mobileMenu.classList.toggle('hidden');
        });
    }
}
