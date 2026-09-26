// Relay Documentation & Showcase Engine

document.addEventListener('DOMContentLoaded', () => {
    initTabs();
    initCopyButtons();
    initNavigation();
    initSearch();
    initMobileMenu();
});

// Dependency Tab Switcher (Kotlin, Groovy, Maven)
function initTabs() {
    const tabButtons = document.querySelectorAll('[data-tab-group]');
    tabButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            const group = btn.getAttribute('data-tab-group');
            const target = btn.getAttribute('data-tab-target');

            // Deactivate siblings in this group
            document.querySelectorAll(`[data-tab-group="${group}"]`).forEach(b => {
                b.classList.remove('bg-sky-500/20', 'text-sky-400', 'border-sky-500');
                b.classList.add('text-slate-400', 'border-transparent');
            });

            // Activate clicked button
            btn.classList.add('bg-sky-500/20', 'text-sky-400', 'border-sky-500');
            btn.classList.remove('text-slate-400', 'border-transparent');

            // Show corresponding panel
            document.querySelectorAll(`[data-tab-panel="${group}"]`).forEach(panel => {
                if (panel.getAttribute('data-panel-id') === target) {
                    panel.classList.remove('hidden');
                } else {
                    panel.classList.add('hidden');
                }
            });
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
                const pre = button.closest('.code-container')?.querySelector('code');
                textToCopy = pre ? pre.innerText : '';
            }

            if (textToCopy) {
                navigator.clipboard.writeText(textToCopy.trim()).then(() => {
                    const originalHtml = button.innerHTML;
                    button.innerHTML = `
                        <svg class="w-4 h-4 text-emerald-400 inline" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path>
                        </svg>
                        <span class="text-xs text-emerald-400 font-medium ml-1">Kopyalandı!</span>
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

    function setActiveDoc(hash) {
        if (!hash || hash === '#' || hash === '#home') {
            document.getElementById('landing-view').classList.remove('hidden');
            document.getElementById('docs-view').classList.add('hidden');
            window.scrollTo({ top: 0, behavior: 'smooth' });
            return;
        }

        // Show docs view
        document.getElementById('landing-view').classList.add('hidden');
        document.getElementById('docs-view').classList.remove('hidden');

        const targetId = hash.replace('#', '');
        let found = false;

        articles.forEach(article => {
            if (article.id === targetId) {
                article.classList.remove('hidden');
                found = true;
            } else {
                article.classList.add('hidden');
            }
        });

        // Update active sidebar link
        sidebarLinks.forEach(link => {
            if (link.getAttribute('href') === hash) {
                link.classList.add('active');
            } else {
                link.classList.remove('active');
            }
        });

        if (found) {
            window.scrollTo({ top: 0, behavior: 'smooth' });
        }
    }

    sidebarLinks.forEach(link => {
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
            const parentSection = link.closest('.sidebar-group');

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
