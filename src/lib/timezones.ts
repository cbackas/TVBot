import * as cityTimeZones from "city-timezones"
const wcc = require('world-countries-capitals')

/**
 * Returns the offset range for the given city or region
 * @param location
 */
export function getTimezone(countryAlpha3: string): string {
  const capitalCity = wcc.getCountryDetailsByISO('alpha_3', countryAlpha3)[0].capital
  const timezone = cityTimeZones.findFromCityStateProvince(capitalCity)

  if (timezone.length !== 1) throw new Error(`Could not find timezone for ${capitalCity}`)

  return timezone[0].timezone
}