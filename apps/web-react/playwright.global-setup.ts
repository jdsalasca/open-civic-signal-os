/**
 * Refuse to run against somebody else's app.
 *
 * Several projects share this machine and several of them serve a Vite app. A civic preview server
 * that dies mid-session lets the next project to start claim the port, and the suite then runs to
 * completion against a university landing page: assertions fail one by one, each pointing at a view
 * that was never loaded. That is a silent failure with a misleading message, and it cost a round to
 * diagnose - the captured error context showed a Spanish university navigation and nothing in the
 * test names hinted at why.
 *
 * One clear failure at the start is worth more than forty confusing ones. The check is a string
 * comparison rather than a selector so it does not depend on any markup of the app under test.
 */
export default async function globalSetup() {
  const baseURL = process.env.BASE_URL || 'http://localhost:3002';

  let title = '';
  try {
    const response = await fetch(`${baseURL}/`);
    title = /<title>([^<]*)<\/title>/i.exec(await response.text())?.[1] ?? '';
  } catch {
    throw new Error(
      `Cannot reach ${baseURL}. Start the app first: npm --prefix apps/web-react run dev`,
    );
  }

  if (!title.includes('Open Civic Signal OS')) {
    throw new Error(
      `${baseURL} is serving "${title || 'a page with no title'}". Another project on this machine is ` +
        'using the port. Point BASE_URL at a civic server or stop the other one before trusting any ' +
        'test result.',
    );
  }
}