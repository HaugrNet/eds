package features

import (
	"fmt"
	"math"
	"strconv"
	"time"
)

// The feature "location" (FEATURES.md, "Location"): where the people of a room
// were last seen. The form of its entries, distances, and the address a
// device opens in its own map app.

// Position is the content of an entry of the kinds "position" and "track",
// and of a signal for a position which isn't kept.
type Position struct {
	V    int     `json:"v"`
	Lat  float64 `json:"lat"`
	Lon  float64 `json:"lon"`
	Acc  int     `json:"acc,omitempty"`
	At   string  `json:"at"`
	Note string  `json:"note,omitempty"`
}

// NewPosition is a position as an entry has it; coordinates to six
// decimals, about 0.1 m. An accuracy of zero or less is left out.
func NewPosition(lat, lon, accuracy float64, at time.Time, note string) Position {
	round := func(value float64) float64 { return math.Round(value*1e6) / 1e6 }
	position := Position{V: 1, Lat: round(lat), Lon: round(lon), At: at.UTC().Format("2006-01-02T15:04:05.000Z"), Note: note}
	if accuracy > 0 {
		position.Acc = int(math.Round(accuracy))
	}
	return position
}

// Valid says whether a position can be shown.
func (p Position) Valid() bool {
	_, err := time.Parse(time.RFC3339, p.At)
	return err == nil && math.Abs(p.Lat) <= 90 && math.Abs(p.Lon) <= 180
}

// Distance is the distance between two places in metres, along the surface
// of the earth (haversine).
func Distance(lat1, lon1, lat2, lon2 float64) float64 {
	radians := func(degrees float64) float64 { return degrees * math.Pi / 180 }
	dLat, dLon := radians(lat2-lat1), radians(lon2-lon1)
	h := math.Pow(math.Sin(dLat/2), 2) + math.Cos(radians(lat1))*math.Cos(radians(lat2))*math.Pow(math.Sin(dLon/2), 2)
	return 2 * 6371008.8 * math.Asin(math.Min(1, math.Sqrt(h)))
}

// GeoLink is the address of a place for the device's own map app (RFC
// 5870).
func GeoLink(p Position) string {
	link := "geo:" + strconv.FormatFloat(p.Lat, 'f', -1, 64) + "," + strconv.FormatFloat(p.Lon, 'f', -1, 64)
	if p.Acc > 0 {
		link += ";u=" + strconv.Itoa(p.Acc)
	}
	return link
}

// Coordinates is a place as numbers: "52.52000° N, 13.40500° E".
func Coordinates(p Position) string {
	part := func(value float64, positive, negative string) string {
		if value >= 0 {
			return fmt.Sprintf("%.5f° %s", value, positive)
		}
		return fmt.Sprintf("%.5f° %s", -value, negative)
	}
	return part(p.Lat, "N", "S") + ", " + part(p.Lon, "E", "W")
}
