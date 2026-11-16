import { Fragment } from 'react';

/**
 * Renders user-written text with safe HTTP(S) links.
 *
 * <p>A feed is read as text, so a pasted link must be clickable without turning the whole card into
 * a link or trusting HTML. Only http/https becomes an anchor; the rest stays a text node, and
 * `rel="noopener noreferrer"` keeps a new tab from reaching back into the app.
 */
export function LinkifiedText({ text }: { text: string }) {
  const parts = text.split(/(https?:\/\/[^\s]+)/g);

  return (
    <>
      {parts.map((part, index) => {
        if (!/^https?:\/\//i.test(part)) {
          return <Fragment key={index}>{part}</Fragment>;
        }
        const trailing = part.match(/[.,;:!?)]+$/)?.[0] ?? '';
        const href = trailing ? part.slice(0, -trailing.length) : part;
        return (
          <Fragment key={index}>
            <a
              className="civic-inline-link"
              href={href}
              target="_blank"
              rel="noopener noreferrer"
            >
              {href}
            </a>
            {trailing}
          </Fragment>
        );
      })}
    </>
  );
}
