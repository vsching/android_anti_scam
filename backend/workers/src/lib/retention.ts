// Retention cron handler — deletes stale data from D1.
// Wired as a Workers scheduled handler via [triggers] crons in wrangler.toml.
//
// Policy:
// - Reports older than 90 days → deleted
// - Pending discoveries older than 30 days → deleted

export async function handleRetention(env: Env): Promise<void> {
  const now = new Date();

  const ninetyDaysAgo = new Date(now.getTime() - 90 * 24 * 60 * 60 * 1000);
  const thirtyDaysAgo = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000);

  await env.DB.batch([
    env.DB.prepare('DELETE FROM reports WHERE created_at < ?').bind(
      ninetyDaysAgo.toISOString(),
    ),
    env.DB.prepare(
      'DELETE FROM pending_discoveries WHERE created_at < ?',
    ).bind(thirtyDaysAgo.toISOString()),
  ]);
}
