import asyncio
from playwright.async_api import async_playwright
import sys

async def main():
    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=True)
        ctx = await browser.new_context(
            viewport={"width": 1440, "height": 900},
            storage_state=None
        )
        page = await ctx.new_page()
        page.set_default_timeout(30000)

        print("[1] Loading login page...")
        await page.goto("https://nttrust.onrender.com/usa/login.html")
        await page.wait_for_load_state("networkidle")
        print(f"    Title: {await page.title()}")

        print("[2] Submitting credentials...")
        await page.fill("#userId", "2214578903")
        await page.fill("#password", "Alex$kj1985!4200")
        await page.click("#submitLogin")
        await asyncio.sleep(4)

        otp_modal = await page.query_selector("#loginOtpModal")
        otp_hidden = await otp_modal.get_attribute("hidden") if otp_modal else "true"
        if otp_modal and otp_hidden is None:
            print("    OTP modal visible. OTP has been sent to Telegram.")
            print("    PAUSE: waiting for OTP code via storage...")
        else:
            print("    No OTP modal, checking redirect...")
            print(f"    URL: {page.url}")
            await browser.close()
            return

        # Save browser state so we can resume
        await ctx.storage_state(path="C:/Users/Administrator/Downloads/northerns/test_state.json")
        print("    State saved.")
        await browser.close()

asyncio.run(main())
