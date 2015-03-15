This mobile-friendly web application helps you identify tracks heard on the radio. It was developed for 7digital's developer challenge.

It shows you the tracks most recently played on the BBC's music stations. You can listen to previews of each track (if available - see icon to right of track name) and follow a link to the relevant page on 7digital's mobile music store.

It is written using Java, the jQuery Mobile framework, the BBC Radio Developers API and the 7digital developer platform. It is hosted on Google App Engine.

Track information is updated every 60 seconds - refresh the page to get the latest list. Wherever possible the name of the current show is displayed too. 7digital API requests are cached. Track lists are not available for every presenter and thumbnail/preview availability depends on the specific song. The application attempts to link out to the specific track in the 7digital store, but if no exact match can be found then it shows search results instead.

[![](http://qrcode.kaywa.com/img.php?s=5&d=http%3A%2F%2Fgoo.gl%2F6P5XM&dummy=.png)](http://goo.gl/6P5XM)

Future plans:
  * Integration with other services (Spotify, Last.fm...)
  * Push updates for latest tracks
  * In-app purchases
  * Support for other stations
  * RadioTAG/RadioDNS support?