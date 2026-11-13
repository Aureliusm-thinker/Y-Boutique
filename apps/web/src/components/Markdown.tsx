import Link from "next/link";
import { Fragment, type ReactNode } from "react";

/**
 * A deliberately small Markdown renderer for staff-written pages: "## headings", "- lists", paragraphs,
 * **bold** and [links](/path). It builds React elements (never raw HTML), so content can't inject scripts.
 */
export function Markdown({ source }: { source: string }) {
  const blocks = source.replace(/\r\n/g, "\n").split(/\n{2,}/).map((b) => b.trim()).filter(Boolean);
  return (
    <div className="space-y-4 leading-relaxed text-stone-700">
      {blocks.map((block, i) => {
        if (block.startsWith("## ") || block.startsWith("# ")) {
          const [heading, ...rest] = block.split("\n");
          return (
            <Fragment key={i}>
              <h2 className="pt-4 font-serif text-2xl text-stone-900">{inline(heading.replace(/^#+\s*/, ""))}</h2>
              {rest.length > 0 && <Paragraphs lines={rest} />}
            </Fragment>
          );
        }
        return <Paragraphs key={i} lines={block.split("\n")} />;
      })}
    </div>
  );
}

function Paragraphs({ lines }: { lines: string[] }) {
  const out: ReactNode[] = [];
  let list: string[] = [];
  let text: string[] = [];
  const flush = () => {
    if (list.length) {
      out.push(
        <ul key={out.length} className="list-disc space-y-1 pl-5">
          {list.map((l, i) => (
            <li key={i}>{inline(l)}</li>
          ))}
        </ul>,
      );
      list = [];
    }
    if (text.length) {
      out.push(<p key={out.length}>{inline(text.join(" "))}</p>);
      text = [];
    }
  };
  for (const line of lines) {
    if (/^[-*]\s+/.test(line)) {
      if (text.length) flush();
      list.push(line.replace(/^[-*]\s+/, ""));
    } else {
      if (list.length) flush();
      text.push(line);
    }
  }
  flush();
  return <>{out}</>;
}

function inline(text: string): ReactNode[] {
  const parts: ReactNode[] = [];
  const pattern = /\*\*(.+?)\*\*|\[([^\]]+)\]\(([^)\s]+)\)/g;
  let last = 0;
  for (const m of text.matchAll(pattern)) {
    if (m.index > last) parts.push(text.slice(last, m.index));
    if (m[1] !== undefined) {
      parts.push(<strong key={m.index} className="text-stone-900">{m[1]}</strong>);
    } else {
      const href = m[3];
      const safe = href.startsWith("/") || /^https?:\/\//.test(href) || href.startsWith("mailto:");
      parts.push(
        safe ? (
          href.startsWith("/") ? (
            <Link key={m.index} href={href} className="underline">
              {m[2]}
            </Link>
          ) : (
            <a key={m.index} href={href} className="underline" rel="noopener noreferrer">
              {m[2]}
            </a>
          )
        ) : (
          m[2]
        ),
      );
    }
    last = m.index + m[0].length;
  }
  if (last < text.length) parts.push(text.slice(last));
  return parts;
}
