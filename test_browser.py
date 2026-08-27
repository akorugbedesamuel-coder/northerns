import asyncio
from playwright.async_api import async_playwright

async def main():
    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=True)
        page = await browser.new_page(viewport={"width": 1440, "height": 900})
        page.set_default_timeout(30000)
        results = []

        def check(label, result):
            status = "PASS" if result else "FAIL"
            results.append((status, label))
            print(f"    [{status}] {label}", flush=True)

        # Inject auth
        print("[1] Loading dashboard...", flush=True)
        await page.goto("https://nttrust.onrender.com/usa/login.html")
        await page.evaluate("""() => {
            sessionStorage.setItem('nt_account_number', '2214578903');
            sessionStorage.setItem('nt_login_verified', 'true');
        }""")
        await page.goto("https://nttrust.onrender.com/usa/index.html")
        await page.wait_for_load_state("networkidle")
        await asyncio.sleep(12)

        url = page.url
        check("Dashboard loaded", "index" in url)

        # Check NTApi state
        state = await page.evaluate("window.NTApi?.state")
        check("API connected", state and state.get("connected"))
        check("API ready", state and state.get("ready"))

        # Dashboard content
        content = await page.content()
        check("Northern Trust branding", "Northern Trust" in content)
        check("Logo SVG", "logo.svg" in content)
        check("Favicon", "favicon.svg" in content)
        check("NT green #115740", "115740" in content)
        check("Balance content", "balance" in content.lower())
        check("Greeting visible", "Good morning" in content or "Good afternoon" in content or "Good evening" in content or "greeting" in content.lower())
        check("Sidebar nav", "sidebar" in content.lower())
        check("FDIC", "FDIC" in content)
        check("Quick actions", "quick-action" in content.lower())
        check("Sub-account CHK-221457001", "CHK-221457001" in content or "4001" in content)
        check("Sub-account SAV-221457002", "SAV-221457002" in content or "4002" in content)
        check("Sub-account CRD-221457003", "CRD-221457003" in content or "4003" in content)
        check("Sub-account INV-221457004", "INV-221457004" in content or "4004" in content)

        # mockOtpDemoBlock - check if properly hidden when connected
        mock_hidden = await page.evaluate("""() => {
            const el = document.getElementById('mockOtpDemoBlock');
            if (!el) return 'not-found';
            const style = window.getComputedStyle(el);
            const parent = el.closest('.nt-api-hidden, [hidden]');
            return `display=${style.display}, parentHidden=${!!parent}, classes=${el.className}`;
        }""")
        check("mockOtpDemoBlock hidden when connected", "parentHidden=True" in mock_hidden or "not-found" in mock_hidden or "display=none" in mock_hidden)

        await page.screenshot(path="C:/Users/Administrator/Downloads/northerns/test-dashboard.png", full_page=True)

        # Navigate to Beneficiaries section
        print("\n[2] Beneficiaries section...", flush=True)
        try:
            # Click Beneficiaries in sidebar
            await page.click("text=Beneficiaries")
            await asyncio.sleep(5)
            ben_content = await page.content()
            check("Lindqvist & Strand Law LLP", "Lindqvist" in ben_content)
            check("Skarsgard Reserve Sweep", "Skarsgard Reserve" in ben_content or "Reserve Sweep" in ben_content)
            check("Gustafsson Properties AB", "Gustafsson" in ben_content)
            check("Meridian Capital Partners", "Meridian" in ben_content)
            check("Swedish Red Cross Foundation", "Swedish Red Cross" in ben_content or "Red Cross" in ben_content)
            check("Lloyd's of London Syndicate", "Lloyd" in ben_content)
            await page.screenshot(path="C:/Users/Administrator/Downloads/northerns/test-beneficiaries.png", full_page=True)
        except Exception as e:
            print(f"    Error: {e}", flush=True)
            for n in ["Lindqvist", "Skarsgard Reserve", "Gustafsson", "Meridian", "Swedish Red Cross", "Lloyd"]:
                check(f"{n} (beneficiaries)", False)

        # Navigate to Transfer section
        print("\n[3] Transfer section...", flush=True)
        try:
            await page.click("text=Cash Movement and Trading")
            await asyncio.sleep(1)
            # Check submenu
            submenu_text = await page.evaluate("""() => {
                const sub = document.getElementById('sidebarCashSubmenu');
                return sub ? sub.innerText : '';
            }""")
            print(f"    Submenu: {submenu_text[:200]}", flush=True)
            check("Internal Transfer option", "Internal Transfer" in submenu_text)
            check("Wire Transfer option", "Wire" in submenu_text)
            check("ACH Transfer option", "ACH" in submenu_text)
            check("International Transfer option", "International" in submenu_text or "SWIFT" in submenu_text)
            await page.screenshot(path="C:/Users/Administrator/Downloads/northerns/test-sidebar-transfers.png", full_page=True)
        except Exception as e:
            print(f"    Error: {e}", flush=True)
            for t in ["Internal Transfer", "Wire Transfer", "ACH Transfer", "International Transfer"]:
                check(f"{t}", False)

        # Admin page
        print("\n[4] Admin page...", flush=True)
        await page.goto("https://nttrust.onrender.com/usa/admin.html")
        await page.wait_for_load_state("networkidle")
        check("Admin title", "Treasury" in (await page.title()))
        await page.fill("#adminUserId", "100000")
        await page.fill("#adminPassword", "NT@Admin2026!")
        await page.click("#adminLoginBtn")
        await asyncio.sleep(5)
        ac = await page.content()
        check("Admin console loaded", "admin-stat" in ac.lower() or "pending" in ac.lower() or "statPending" in ac)
        check("Approvals tab", "approval" in ac.lower())
        check("Transfers tab", "transfer" in ac.lower())
        check("Accounts tab", "account" in ac.lower())
        await page.screenshot(path="C:/Users/Administrator/Downloads/northerns/test-admin.png", full_page=True)

        # Summary
        passed = sum(1 for s, _ in results if s == "PASS")
        total = len(results)
        print(f"\n{'='*50}", flush=True)
        print(f"RESULTS: {passed}/{total} passed", flush=True)
        print(f"{'='*50}", flush=True)
        for s, l in results:
            print(f"  [{s}] {l}", flush=True)

        await browser.close()

asyncio.run(main())
