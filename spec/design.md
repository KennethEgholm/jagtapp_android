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

**Sammenfold grupper** står i Indstillinger-fanen. Standard til. “Ikke jagtbar her” foldes stadig.

Artsdetaljen fra Jagtbart viser status. Den har foto og biologi som iOS: fotoet står under overskriften i 3:2 med kreditlinje, biologi (latinsk navn, “I Danmark”, “Yngel” og kildelinks) under noten, før jagtkilden. Billedet ligger i `app/src/main/assets/photos/` som `photo-<id>.jpg`, så et manglende billede bare udelader kortet. Den har ikke journalknap endnu.

## Schweiss-registret

Ikke en fane. **Anskudt**-knappen står i toppen af Jagtbart ved siden af Indstillinger og åbner et `ModalBottomSheet`. Indholdet (`SchweissContent`) genbruges, når Om bliver bygget og skubber det ind. To veje, begge synlige, ingen omskifter: Anskudt under jagt (link til schweiss.dk og Naturstyrelsen) og påkørt hjortevildt (Ring 1812). Telefonnumre åbner opkalds-appen via `ACTION_DIAL`, mail åbner mail-appen, nettet åbner browseren. Seks dagtimer er tekst med kilde. Ingen timer. Ingen telefonliste, ingen scrape. Appen er ikke loven.

## Journal

Anden fane i bundnavigationen (`NavigationBar`): Jagtbart, så Journal. Dato og sted har egen kopi her (`BagDraft`) — de ændrer ikke Jagtbart. Swipe til venstre på en artsrække i Jagtbart noterer ét stykke og skifter til Journal; rækken springer tilbage (`SwipeToDismissBox` med `confirmValueChange` der returnerer falsk — handlingen er en notering, ikke en sletning). Bekræftelsen står nederst: “[Art] noteret”, Fortryd, +1, Se året. Årets liste grupperer på måned, og swipe sletter for alvor dér. Artslisten kan tilføjes fra et ark med søgning. Ingen iCloud: journalen er kun en lokal fil med rullende backup (`vildtudbytte.json`, `artsliste.json`) — Android-delen deler ikke data.

## Alle arter

Tredje fane. Generelle jagttider for alle arter, grupperet i kort der starter sammenfoldet ("Sammenfold grupper" gælder også her). Søgning folder grupperne med træf ud uden ekstra tryk. Arterne er slået op ved Danmark, så ingen lokal tid lækker ind. Ingen dato, intet sted, ingen åben/lukket. Rækkerne har samme journal-swipe som Jagtbart. Detaljesiden viser den generelle kontekst: ingen status, tynd SeasonBar uden markør og artnoten i stedet for stedens note.

## Indstillinger

Fjerde fane (tandhjul). Øverst **Udseende** som en `SingleChoiceSegmentedButtonRow` (System/Lys/Mørk med ikoner) — valget ligger i "udseende"-præferencen og styrer `JagtFokusTheme`, så hele appen skifter med det samme. Så **Lister** med "Sammenfold grupper". Derefter Om-sektionerne i samme liste, som iOS: Data (kilde, opdateret, bekendtgørelsen), Ansvar, Schweiss-registret (skubbet ind som skærm, ikke ark), Fotos, Privatliv og Denne app med Version (fra PackageManager) og Dataversion (fra kataloget).

## Endnu ikke bygget

Velkomst.
