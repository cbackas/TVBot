export const addLeadingZeros = (num: number, totalLength: number): string => {
  return String(num).padStart(totalLength, '0');
}

export function toRanges(values: number[], separator = '\u2013'): string[] {
  return values
    .slice()
    .sort((p, q) => p - q)
    .reduce((acc: number[][], cur, idx, src) => {
      if ((idx > 0) && ((cur - src[idx - 1]) === 1))
        acc[acc.length - 1][1] = cur;
      else acc.push([cur]);
      return acc;
    }, [])
    .map(range => range.join(separator));
}
