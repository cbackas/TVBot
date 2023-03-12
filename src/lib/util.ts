import parseUrl from 'parse-url'

export function addLeadingZeros (num: number, totalLength: number): string {
  return String(num).padStart(totalLength, '0')
}

export function toRanges (values: number[], separator = '-', totalLength = 2): string[] {
  return values
    .slice()
    .sort((p, q) => p - q)
    .reduce((acc: number[][], cur, idx, src) => {
      if ((idx > 0) && ((cur - src[idx - 1]) === 1)) {
        acc[acc.length - 1][1] = cur
      } else {
        acc.push([cur])
      }
      return acc
    }, [])
    .map(range => range.map(value => addLeadingZeros(value, totalLength)).join(separator))
}

export function parseIMDBIds (imdbIds: string): string[] {
  return imdbIds.split(',')
    // filter out invalid imdb ids and handle imdb urls
    .reduce((acc, id) => {
      if (id.startsWith('tt')) return [...acc, id]

      try {
        const parsedUrl = parseUrl(id, true)
        if (parsedUrl.resource === 'imdb.com' && parsedUrl.pathname.startsWith('/title/')) {
          return [...acc, parsedUrl.pathname.split('/title/')[1]]
        }
      } catch (e) { }

      return acc
    }, new Array<string>())
}
