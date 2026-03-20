// Minimal request router — no external dependencies.
// Supports method + path matching with named path parameters.

type RouteHandler = (
  request: Request,
  env: Env,
  params: Record<string, string>,
) => Response | Promise<Response>;

interface Route {
  method: string;
  pattern: string;
  segments: string[];
  handler: RouteHandler;
}

export class Router {
  private routes: Route[] = [];

  /**
   * Register a route handler for a given HTTP method and path pattern.
   * Path params are denoted with a leading colon, e.g. '/api/data/:type'.
   */
  register(method: string, pattern: string, handler: RouteHandler): void {
    const segments = pattern.split('/').filter(Boolean);
    this.routes.push({ method: method.toUpperCase(), pattern, segments, handler });
  }

  /** Convenience helpers */
  get(pattern: string, handler: RouteHandler): void {
    this.register('GET', pattern, handler);
  }

  post(pattern: string, handler: RouteHandler): void {
    this.register('POST', pattern, handler);
  }

  delete(pattern: string, handler: RouteHandler): void {
    this.register('DELETE', pattern, handler);
  }

  /**
   * Match an incoming request against registered routes.
   * Returns the handler response or null if no route matched.
   */
  async match(request: Request, env: Env): Promise<Response | null> {
    const url = new URL(request.url);
    const method = request.method.toUpperCase();
    const pathSegments = url.pathname.split('/').filter(Boolean);

    for (const route of this.routes) {
      if (route.method !== method) continue;
      if (route.segments.length !== pathSegments.length) continue;

      const params: Record<string, string> = {};
      let matched = true;

      for (let i = 0; i < route.segments.length; i++) {
        const routeSeg = route.segments[i];
        const pathSeg = pathSegments[i];

        if (routeSeg.startsWith(':')) {
          params[routeSeg.slice(1)] = decodeURIComponent(pathSeg);
        } else if (routeSeg !== pathSeg) {
          matched = false;
          break;
        }
      }

      if (matched) {
        return route.handler(request, env, params);
      }
    }

    return null;
  }
}
