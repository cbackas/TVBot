import * as cityTimeZones from "npm:city-timezones"

/**
 * Returns the offset range for the given city or region
 * @param location
 */
export function getTimezone(countryAlpha3: string): string {
  const t = cityTimeZones.cityMapping.filter((city) =>
    city.iso3 === countryAlpha3.toUpperCase()
  ).reduce((prev, current) => {
    return (prev.pop > current.pop) ? prev : current
  })

  if (t === undefined) {
    throw new Error(`Could not find timezone for ${countryAlpha3}`)
  }

  return t.timezone
}
