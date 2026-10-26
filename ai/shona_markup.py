"""Explicit caller annotations only; no language guessing or semantic verification."""
import re
PATTERN=re.compile(r'\[\[keep:([^\[\]\r\n]+)\]\]')
PREFIX='982'
def utf16(text):return len(text.encode('utf-16-le'))//2

def protect(source):
    if PREFIX in source:raise ValueError('Reserved placeholder collision')
    matches=list(PATTERN.finditer(source))
    if not 1<=len(matches)<=8:raise ValueError('Expected one to eight explicit keep spans')
    spans=[];parts=[];position=0
    for i,match in enumerate(matches):
        quote=match.group(1)
        if not quote.strip() or utf16(quote)>256:raise ValueError('Invalid keep span')
        parts.append(source[position:match.start()]);sentinel=PREFIX+str(i);parts.append(sentinel);position=match.end()
        spans.append({'sentinel':sentinel,'quote':quote,'sourceStart':utf16(source[:match.start(1)]),'sourceEnd':utf16(source[:match.end(1)])})
    parts.append(source[position:]);masked=''.join(parts)
    if '[[' in masked or ']]' in masked:raise ValueError('Malformed or nested annotation')
    return masked,spans

def restore(output,spans):
    positions=[]
    for span in spans:
        sentinel=span['sentinel'];matches=list(re.finditer(r'(?<![0-9])'+re.escape(sentinel)+r'(?![0-9])',output))
        if len(matches)!=1:raise ValueError('Placeholder missing or duplicated')
        positions.append(matches[0].start())
    if positions!=sorted(positions):raise ValueError('Placeholder order changed')
    parts=[];evidence=[];position=0;length=0
    for span,start in zip(spans,positions):
        prefix=output[position:start];parts.append(prefix);length+=utf16(prefix);quote=span['quote'];begin=length;parts.append(quote);length+=utf16(quote)
        evidence.append({'sourceStart':span['sourceStart'],'sourceEnd':span['sourceEnd'],'outputStart':begin,'outputEnd':length,'quote':quote,'trust':'SOURCE_COPY_UNVERIFIED'});position=start+len(span['sentinel'])
    parts.append(output[position:]);text=''.join(parts)
    if PREFIX in text:raise ValueError('Unconsumed placeholder')
    return text,evidence
