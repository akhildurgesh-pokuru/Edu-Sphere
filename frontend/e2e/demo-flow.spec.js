import { expect, test } from '@playwright/test';

// Walks the complete demo flow from the specification with real data and a real backend.
const run = Date.now();
const teacher = { name: 'Tess Teacher', email: `teacher.${run}@e2e.local`, password: 'Passw0rdX' };
const student = { name: 'Sam Student', email: `student.${run}@e2e.local`, password: 'Passw0rdX' };
const SUBJECT = `Java Programming ${run}`;

const pdf = Buffer.from('%PDF-1.4\n1 0 obj<<>>endobj\n%%EOF');
const mp4 = (() => { const b = Buffer.alloc(4096); b.write('ftyp', 4); return b; })();

async function signUp(page, user, roleLabel) {
  await page.goto('/register');
  await page.locator('label', { hasText: roleLabel }).first().click();
  await page.getByLabel('Full name').fill(user.name);
  await page.getByLabel('Email').fill(user.email);
  await page.getByLabel(/^Password/).fill(user.password);
  await page.getByLabel('Confirm password').fill(user.password);
  await page.getByRole('button', { name: 'Create account' }).click();
}

test.describe.serial('EduSphere demo flow', () => {
  let tctx; let sctx; let tp; let sp; let subjectUrl; let studentCourse;

  test.beforeAll(async ({ browser }) => {
    tctx = await browser.newContext();
    sctx = await browser.newContext();
    tp = await tctx.newPage();
    sp = await sctx.newPage();
  });
  test.afterAll(async () => { await tctx.close(); await sctx.close(); });

  test('1. teacher registers, builds and publishes "Java Programming"', async () => {
    await signUp(tp, teacher, 'Teach');
    await expect(tp).toHaveURL(/\/teacher\/dashboard/);
    await expect(tp.getByRole('heading', { name: /Welcome back, Tess/ })).toBeVisible();
    await expect(tp.getByText('Create your first subject')).toBeVisible(); // empty state with a real, zero-data dashboard

    await tp.getByRole('link', { name: 'New subject' }).first().click();
    await tp.getByLabel('Title').fill(SUBJECT);
    await tp.getByLabel('Category').selectOption('Programming');
    await tp.getByLabel('Description').fill('Learn Java from scratch.');
    await tp.getByRole('button', { name: 'Create subject' }).click();
    await expect(tp).toHaveURL(/\/teacher\/subjects\/\d+$/);
    subjectUrl = tp.url();

    // three modules
    for (const title of ['Getting started', 'Core concepts', 'Reading list']) {
      await tp.getByRole('button', { name: 'Add module' }).first().click();
      const dialog = tp.getByRole('dialog');
      await dialog.getByLabel('Title').fill(title);
      await dialog.getByRole('button', { name: 'Add module' }).click();
      await expect(tp.getByText(title, { exact: true })).toBeVisible();
    }

    // video lesson
    await tp.getByRole('button', { name: 'Add lesson' }).nth(0).click();
    let dialog = tp.getByRole('dialog');
    await dialog.getByLabel('Title').fill('Welcome video');
    await dialog.locator('#lesson-file').setInputFiles({ name: 'welcome.mp4', mimeType: 'video/mp4', buffer: mp4 });
    await expect(dialog.getByText('welcome.mp4')).toBeVisible();
    await dialog.getByLabel('Video length (minutes)').fill('2');
    await dialog.getByLabel(/Publish this lesson/).check();
    await dialog.getByRole('button', { name: 'Add lesson' }).click();
    await expect(tp.getByText('Welcome video')).toBeVisible();

    // PDF lesson
    await tp.getByRole('button', { name: 'Add lesson' }).nth(1).click();
    dialog = tp.getByRole('dialog');
    await dialog.getByLabel('Title').fill('Cheat sheet');
    await dialog.getByLabel('Lesson type').selectOption('DOCUMENT');
    await dialog.locator('#lesson-file').setInputFiles({ name: 'cheatsheet.pdf', mimeType: 'application/pdf', buffer: pdf });
    await expect(dialog.getByText('cheatsheet.pdf')).toBeVisible();
    await dialog.getByLabel(/Publish this lesson/).check();
    await dialog.getByRole('button', { name: 'Add lesson' }).click();
    await expect(tp.getByText('Cheat sheet')).toBeVisible();

    // text lesson
    await tp.getByRole('button', { name: 'Add lesson' }).nth(2).click();
    dialog = tp.getByRole('dialog');
    await dialog.getByLabel('Title').fill('Recommended books');
    await dialog.getByLabel('Lesson type').selectOption('TEXT');
    await dialog.getByLabel('Lesson text').fill('Effective Java by Joshua Bloch.');
    await dialog.getByLabel(/Publish this lesson/).check();
    await dialog.getByRole('button', { name: 'Add lesson' }).click();
    await expect(tp.getByText('Recommended books')).toBeVisible();

    // quiz with 10 questions
    await tp.getByRole('tab', { name: 'Quizzes' }).click();
    await tp.getByRole('button', { name: 'New quiz' }).click();
    dialog = tp.getByRole('dialog');
    await dialog.getByLabel('Title').fill('Java basics quiz');
    await dialog.getByLabel('Max attempts').fill('2');
    await dialog.getByRole('button', { name: 'Create quiz' }).click();
    await tp.getByRole('link', { name: 'Java basics quiz' }).click();
    await expect(tp).toHaveURL(/\/teacher\/quizzes\/\d+$/);
    for (let i = 1; i <= 9; i++) {
      await tp.getByRole('button', { name: 'Add question' }).first().click();
      dialog = tp.getByRole('dialog');
      await dialog.getByLabel(/^Question/).fill(`Question ${i}: which answer is correct?`);
      await dialog.getByLabel('Option 1 text').fill('The right answer');
      await dialog.getByLabel('Option 2 text').fill('A wrong answer');
      await dialog.getByRole('button', { name: 'Add question' }).click();
      await expect(tp.getByText(`Question ${i}: which`)).toBeVisible();
    }
    await tp.getByRole('button', { name: 'Add question' }).first().click();
    dialog = tp.getByRole('dialog');
    await dialog.getByLabel('Type').selectOption('SHORT_ANSWER');
    await dialog.getByLabel(/^Question/).fill('Question 10: explain polymorphism in one sentence.');
    await dialog.getByLabel('Marks').fill('2');
    await dialog.getByRole('button', { name: 'Add question' }).click();
    await expect(tp.getByText('Question 10')).toBeVisible();
    await tp.getByRole('button', { name: 'Publish quiz' }).click();
    await expect(tp.getByText('Published', { exact: true }).first()).toBeVisible();

    // assignment + announcement
    await tp.goto(`${subjectUrl}?tab=assignments`);
    await tp.getByRole('button', { name: 'New assignment' }).first().click();
    dialog = tp.getByRole('dialog');
    await dialog.getByLabel('Title').fill('Build a calculator');
    await dialog.getByLabel('Due date').fill('2030-01-01T10:00');
    await dialog.getByRole('button', { name: 'Create assignment' }).click();
    await expect(tp.getByText('Build a calculator')).toBeVisible();

    await tp.goto(`${subjectUrl}?tab=announcements`);
    await tp.getByRole('button', { name: 'New announcement' }).click();
    dialog = tp.getByRole('dialog');
    await dialog.getByLabel('Title').fill('Welcome to the course');
    await dialog.getByLabel('Message').fill('Introductions in the forum please.');
    await dialog.getByRole('button', { name: 'Post' }).click();
    await expect(tp.getByText('Welcome to the course')).toBeVisible();

    // publish
    await tp.goto(subjectUrl);
    await tp.getByRole('button', { name: 'Publish', exact: true }).click();
    await expect(tp.getByRole('button', { name: 'Unpublish' })).toBeVisible();
  });

  test('2. student finds the course, enrolls and learns', async () => {
    await signUp(sp, student, 'Learn');
    await expect(sp).toHaveURL(/\/student\/dashboard/);
    await sp.getByRole('navigation', { name: 'Main' }).first().getByRole('link', { name: 'Browse Courses' }).click();
    await sp.getByLabel('Search courses').fill(`Java Programming ${run}`);
    await expect(sp.getByText('1 course found')).toBeVisible(); // search is debounced; wait for the filtered list
    await expect(sp.getByRole('link', { name: SUBJECT }).first()).toBeVisible();
    await sp.getByRole('button', { name: 'Enroll now' }).click();
    await expect(sp.getByText(`Enrolled in ${SUBJECT}`)).toBeVisible();

    await sp.getByRole('navigation', { name: 'Main' }).first().getByRole('link', { name: 'My Subjects' }).click();
    await sp.getByRole('link', { name: 'Open course' }).click();
    await expect(sp.getByRole('heading', { name: SUBJECT })).toBeVisible();
    studentCourse = new URL(sp.url()).pathname;
    await sp.getByRole('link', { name: /Start learning/ }).click();
    await expect(sp).toHaveURL(/\/learn\/\d+$/);
    await expect(sp.getByRole('heading', { name: 'Welcome video' })).toBeVisible();
    await expect(sp.locator('video')).toBeVisible();
    await sp.getByRole('button', { name: 'Mark complete' }).click();
    await expect(sp.getByRole('button', { name: 'Completed' })).toBeVisible();

    // download the PDF
    await sp.getByRole('button', { name: 'Next' }).click();
    await expect(sp.getByRole('heading', { name: 'Cheat sheet' })).toBeVisible();
    const link = sp.getByRole('link', { name: 'Download' }).first();
    const [download] = await Promise.all([sp.waitForEvent('download'), link.click()]);
    expect(download.suggestedFilename()).toBe('cheatsheet.pdf');

    // quiz: answer the nine single choice questions (first option is correct) and the short answer
    await sp.goto(`${studentCourse}?tab=quizzes`);
    await sp.getByRole('button', { name: 'Start quiz' }).click();
    await expect(sp.getByRole('timer')).toBeVisible();
    for (let i = 1; i <= 9; i++) {
      await sp.getByLabel('The right answer').check();
      await sp.getByRole('button', { name: /Next/ }).click();
    }
    await sp.getByLabel('Your answer').fill('One interface, many implementations.');
    await sp.getByRole('button', { name: /Submit quiz/ }).click();
    await sp.getByRole('dialog').getByRole('button', { name: 'Submit now' }).click();
    await expect(sp.getByText('Short answers are waiting for your teacher')).toBeVisible();
    await expect(sp.getByText('Review your answers')).toBeVisible();

    // assignment
    await sp.goto(`${studentCourse}?tab=assignments`);
    await sp.getByLabel('Choose file for Build a calculator').setInputFiles({ name: 'calculator.pdf', mimeType: 'application/pdf', buffer: pdf });
    await expect(sp.getByText('Submitted. Your teacher has been notified.')).toBeVisible();
    await expect(sp.getByText('calculator.pdf').first()).toBeVisible();

    // forum
    await sp.goto(`${studentCourse}?tab=discussions`);
    await sp.getByRole('button', { name: 'New discussion' }).click();
    const dialog = sp.getByRole('dialog');
    await dialog.getByLabel('Title').fill('How do generics work?');
    await dialog.getByLabel('Message').fill('<img src=x onerror=alert(1)> please explain');
    await dialog.getByRole('button', { name: 'Post' }).click();
    // user text is shown escaped, never executed
    await expect(sp.getByText('<img src=x onerror=alert(1)> please explain')).toBeVisible();
    expect(await sp.locator('img[src="x"]').count()).toBe(0);

    // progress
    await sp.getByRole('navigation', { name: 'Main' }).first().getByRole('link', { name: 'My Progress' }).click();
    await expect(sp.getByText('Overall progress')).toBeVisible();
    await expect(sp.getByText(SUBJECT).first()).toBeVisible();
  });

  test('3. teacher sees the enrollment, grades work and reads results and analytics', async () => {
    await tp.goto('/teacher/dashboard');
    await expect(tp.getByText('Enrolled students').first()).toBeVisible();
    await expect(tp.getByRole('heading', { name: 'Recent enrollments' }).or(tp.getByText('Recent enrollments'))).toBeVisible();
    await expect(tp.getByText(student.name).first()).toBeVisible();

    // grade the assignment
    await tp.goto(`${subjectUrl}?tab=assignments`);
    await tp.getByRole('link', { name: 'Submissions' }).click();
    await expect(tp.getByText(student.name)).toBeVisible();
    await tp.getByRole('button', { name: 'Grade' }).click();
    const dialog = tp.getByRole('dialog');
    await dialog.getByLabel(/Marks/).fill('92');
    await dialog.getByLabel('Feedback').fill('Great work');
    await dialog.getByRole('button', { name: 'Save marks' }).click();
    await expect(tp.getByText('92 / 100')).toBeVisible();

    // quiz results: grade the short answer
    await tp.goto(`${subjectUrl}?tab=quizzes`);
    await tp.getByRole('link', { name: 'Results' }).click();
    await expect(tp.getByText(student.name)).toBeVisible();
    await tp.getByRole('link', { name: 'Grade' }).click();
    await tp.getByLabel(/Marks \(max 2\)/).fill('2');
    await tp.getByRole('button', { name: 'Save grades' }).click();
    await expect(tp.getByText('Grades saved')).toBeVisible();

    // analytics
    await tp.goto(`${subjectUrl}?tab=analytics`);
    await expect(tp.getByText('Completion rate')).toBeVisible();
    await expect(tp.getByText(student.email)).toBeVisible();
  });

  test('4. security: the student cannot open teacher pages or other people\'s data', async () => {
    await sp.goto('/teacher/dashboard');
    await expect(sp).toHaveURL(/\/unauthorized/);
    const status = await sp.evaluate(async () => (await fetch('http://localhost:8080/api/teacher/subjects', { credentials: 'include' })).status);
    expect(status).toBe(403);
  });
});

test('mobile layout has no horizontal scroll and a working drawer', async ({ browser }) => {
  const ctx = await browser.newContext({ viewport: { width: 375, height: 812 } });
  const page = await ctx.newPage();
  await page.goto('/login');
  await page.getByLabel('Email').fill('teacher@edusphere.local');
  await page.getByLabel(/^Password/).fill('Teacher@12345');
  await page.getByRole('button', { name: 'Sign in' }).click();
  await expect(page).toHaveURL(/\/teacher\/dashboard/);
  for (const path of ['/teacher/dashboard', '/teacher/subjects', '/notifications', '/profile']) {
    await page.goto(path);
    await page.waitForLoadState('networkidle');
    const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
    expect(overflow, `horizontal overflow on ${path}`).toBeLessThanOrEqual(1);
  }
  await page.getByRole('button', { name: 'Open menu' }).click();
  await expect(page.getByRole('navigation', { name: 'Main' }).last()).toBeVisible();
  await ctx.close();
});
