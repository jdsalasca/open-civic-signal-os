import { test, expect } from '@playwright/test';

const BASE_URL = 'http://localhost:3002';

test.describe('Signal OS - High Quality E2E Suite', () => {

  test('Complete Quality Audit Flow', async ({ page }) => {
    const uniqueUser = `qa_expert_${Date.now()}`;
    
    // 1. Semantic Auth Verification
    await page.goto(`${BASE_URL}/login`);
    await page.getByTestId('go-to-register').click();
    await expect(page).toHaveURL(/.*register/);

    await page.locator('#username-input').fill(uniqueUser);
    await page.locator('#email-input').fill(`${uniqueUser}@signalos.org`);
    await page.locator('#password-input').fill('SecurePass123!');
    await page.getByTestId('register-submit-button').click();

    // 2. Successful Login & Identity Verification
    await expect(page).toHaveURL(/.*login/);
    await page.locator('#username-input').fill(uniqueUser);
    await page.locator('#password-input').fill('SecurePass123!');
    await page.getByTestId('login-submit-button').click();

    // Language Agnostic Identity Welcome
    await expect(page.getByTestId('welcome-message')).toContainText(uniqueUser, { timeout: 15000 });

    // 3. Settings: i18n & Theme Hardening
    await page.goto(`${BASE_URL}/settings`);
    
    // Select Spanish using robust selector
    await page.locator('.p-selectbutton >> text=Español').click();
    await expect(page.getByText('Configuración del Sistema')).toBeVisible();

    // Select Light Theme
    await page.locator('.p-selectbutton >> text=Modo Claro').click();
    await expect(page.locator('html')).toHaveClass(/light-theme/);

    // 4. Contribution Flow (Spanish)
    await page.click('a[href="/report"]');
    await page.locator('#title').fill('Mejora de Alumbrado Público');
    await page.locator('#description').fill('Validación de flujo bilingüe y estabilidad de datos.');
    
    // PrimeReact Dropdown handle
    await page.locator('.p-dropdown').click();
    await page.locator('.p-dropdown-item >> text=Infraestructura').click();
    
    await page.getByRole('button', { name: 'Ingresar Señal' }).click();

    // 5. Dashboard Integrity (Zero Ghost Rows)
    await expect(page.locator('[data-testid="dashboard-hero"]')).toBeVisible();
    const table = page.locator('[data-testid="signals-datatable"]');
    await expect(table).toBeVisible();
    
    // Strategic verification: Ensure no skeletons stick
    await expect(page.locator('.p-skeleton')).toHaveCount(0, { timeout: 10000 });
    await expect(table).toContainText('Mejora de Alumbrado Público');

    // 6. Security & Cleanup
    await page.locator('button[aria-label="Cerrar Sesión"]').click();
    await expect(page).toHaveURL(/.*login/);
  });
});
