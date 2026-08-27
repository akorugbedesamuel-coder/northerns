import asyncio
from playwright.async_api import async_playwright

async def main():
    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=True)
        page = await browser.new_page(viewport={"width": 1440, "height": 900})
        page.set_default_timeout(30000)

        # Collect console logs
        logs = []
        page.on("console", lambda msg: logs.append(f"[{msg.type}] {msg.text}"))

        await page.goto("https://nttrust.onrender.com/usa/login.html")
        await page.evaluate("""() => {
            sessionStorage.setItem('nt_account_number', '2214578903');
            sessionStorage.setItem('nt_login_verified', 'true');
        }""")
        await page.goto("https://nttrust.onrender.com/usa/index.html")
        await page.wait_for_load_state("networkidle")
        await asyncio.sleep(12)

        print("=== CONSOLE LOGS ===")
        for l in logs:
            if 'NT' in l or 'error' in l.lower() or 'fail' in l.lower() or 'unreachable' in l.lower():
                print(f"  {l[:200]}")

        print("\n=== STATE ===")
        state = await page.evaluate("""() => {
            if (window.NTApi) return { connected: NTApi.state.connected, ready: NTApi.state.ready };
            return 'NTApi not found';
        }""")
        print(f"  {state}")

        # Check if connection banner is visible
        banner = await page.evaluate("""() => {
            const b = document.getElementById('ntConnectionBanner');
            return b ? { visible: b.classList.contains('visible'), text: b.innerText } : 'not found';
        }""")
        print(f"  Banner: {banner}")

        await browser.close()

asyncio.run(main())
