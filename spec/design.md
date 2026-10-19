# JagtFokus Android — designafvigelser

Produktet og den danske kopi følger iOS-appen i `../jagtapp`. Dette dokument er kun de steder, Android gør noget andet.

## Systemet først

Material 3. Ikke en kopi af SwiftUI. Ingen glas på indhold. Systemets søjler og kontroller må bruge deres egen flade.

- `LargeTopAppBar` med titlen “Jagtbart”. Den lange overskrift står i indholdet.
- Kort er `Surface` med 16 dp hjørner. 24 dp mellem kortene.
- Dato: Material `DatePicker` plus genvejene I dag, I morgen og weekenden.
- Sted: `ModalBottomSheet` med søgning.
- Ikoner er Material Symbols. Gruppeikoner er de samme stregtegninger som iOS.
- Trykflader mindst 48 dp. Tekst i `sp`, så den følger skriftstørrelsen.
- Fast skovgrøn, amber og messing. Ikke Material You. Farverne betyder status.
- Kun grov position. Stederne er regioner.

## Jagtbart

Solkortet står mellem sted og svar. Tryk åbner Sol og vejr. Vejr kommer fra MET og er kun visning. Træk for at opdatere henter også ny jagttid, når versionen er nyere.

Solkortet følger iOS' SunRow: orange flise med sol over horisont, klokkeslæt i primærfarven med orange solhændelsesglyffer, så himmellinjen (“Maks · Min · mm”), vindlinjen (“Op til … m/s fra …”) og månen med faseglyf. Solen er orange (token `Sun`), aldrig grøn — vejr er ikke status.

Ikon-nærvær, fordi Material-sættet mangler SF-symboler: solopgang/solnedgang og sol-over-horisont er tegnet på Canvas (`SunEventIcon`, `sunHorizonIcon`), og månefasernes otte glyffer er tegnet på Canvas (`MoonPhaseIcon`). MET's symboltabel er kortlagt til Material-ikoner i `ui/WeatherIcon.kt`. Vindpilen peger derhen, hvor vinden blæser: vind fra øst peger mod vest.

**Sammenfold grupper** ligger i et ark fra Indstillinger i toppen, indtil Indstillinger-fanen findes. Standard til. “Ikke jagtbar her” foldes stadig.

Artsdetaljen fra Jagtbart viser status. Den har foto og biologi som iOS: fotoet står under overskriften i 3:2 med kreditlinje, biologi (latinsk navn, “I Danmark”, “Yngel” og kildelinks) under noten, før jagtkilden. Billedet ligger i `app/src/main/assets/photos/` som `photo-<id>.jpg`, så et manglende billede bare udelader kortet. Den har ikke journalknap endnu.

## Schweiss-registret

Ikke en fane. **Anskudt**-knappen står i toppen af Jagtbart ved siden af Indstillinger og åbner et `ModalBottomSheet`. Indholdet (`SchweissContent`) genbruges, når Om bliver bygget og skubber det ind. To veje, begge synlige, ingen omskifter: Anskudt under jagt (link til schweiss.dk og Naturstyrelsen) og påkørt hjortevildt (Ring 1812). Telefonnumre åbner opkalds-appen via `ACTION_DIAL`, mail åbner mail-appen, nettet åbner browseren. Seks dagtimer er tekst med kilde. Ingen timer. Ingen telefonliste, ingen scrape. Appen er ikke loven.

## Endnu ikke bygget

Journal, øvrige faner, onboarding. Ingen bundnavigation med én fane.
