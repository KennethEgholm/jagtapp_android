# JagtFokus Android — designafvigelser

Produktet og den danske kopi følger iOS-appen i `../jagtapp`. Dette dokument er kun de steder, Android gør noget andet.

## Navigation og tilstand

Ingen Navigation Compose. Hver fane har sin egen rute i `rememberSaveable` (kun id'er) og sin egen `SaveableStateHolder`, så rutevalg, søgning og rulleposition overlever fanevalg og rotation. Tilbage på en anden fane end Jagtbart går til Jagtbart; en skærms egen tilbage-handling går forud. Tryk på den åbne fane går tilbage til fanens rod og op til toppen. Ulempen er, at der ingen forudsigende tilbage-animation er mellem sider.

Store topbjælker (`LargeTopAppBar`) på Journal, Alle arter, Indstillinger og Sol og vejr folder sig sammen ved rulning. Tastaturet løfter både bjælke og indhold (`imePadding`).

## Systemet først

Material 3. Ikke en kopi af SwiftUI. Ingen glas på indhold. Systemets søjler og kontroller må bruge deres egen flade.

- `LargeTopAppBar` med titlen “Jagtbart”. Den lange overskrift står i indholdet.
- Kort er `Surface` med 16 dp hjørner. 24 dp mellem kortene.
- Dato: Material `DatePicker` plus genvejene I dag, I morgen og weekenden.
- Sted: `ModalBottomSheet` med søgning.
- Ikoner er Material Icons (`material-icons-extended`), ikke Material Symbols. Gruppeikoner er de samme stregtegninger som iOS (`ui/GroupSketch.kt`, tegnet på Canvas).
- Trykflader mindst 48 dp. Tekst i `sp`, så den følger skriftstørrelsen.
- Fast skovgrøn, amber og messing. Ikke Material You. Farverne betyder status.
- Kun grov position. Stederne er regioner.

## Jagtbart

Solkortet står mellem sted og svar. Tryk åbner Sol og vejr. Vejr kommer fra MET og er kun visning. Træk for at opdatere henter også ny jagttid, når versionen er nyere.

Solkortet følger iOS' SunRow: orange flise med sol over horisont, klokkeslæt i primærfarven med orange solhændelsesglyffer, så himmellinjen (“Maks · Min · mm”), vindlinjen (“Op til … m/s fra …”) og månen med faseglyf. Solen er orange (token `Sun`), aldrig grøn — vejr er ikke status.

Ikon-nærvær, fordi Material-sættet mangler SF-symboler: solopgang/solnedgang og sol-over-horisont er tegnet på Canvas (`SunEventIcon`, `sunHorizonIcon`), og månefasernes otte glyffer er tegnet på Canvas (`MoonPhaseIcon`). MET's symboltabel er kortlagt til Material-ikoner i `ui/WeatherIcon.kt`. Vindpilen peger derhen, hvor vinden blæser: vind fra øst peger mod vest.

**Sammenfold grupper** står i Indstillinger-fanen. Standard til. “Ikke jagtbar her” foldes stadig.

Artsdetaljen fra Jagtbart viser status. Den har en lille topbjælke (`TopAppBar`) uden titel; artens navn står som overskrift i indholdet med gruppen som linje over, og kommer først frem i bjælken, når overskriften er rullet væk. Det svarer til iOS' enkelte titel. Linkrækker (kilder, fotokredit) er hele rækken som trykflade med `Role.Button`, fordi Compose ikke har `Role.Link`. Den har foto og biologi som iOS: fotoet står under overskriften i 3:2 med kreditlinje, biologi (latinsk navn, “I Danmark”, “Yngel” og kildelinks) under noten, før jagtkilden. Billedet ligger i `app/src/main/assets/photos/` som `photo-<id>.jpg`, så et manglende billede bare udelader kortet. Den har rækken **Tilføj til journal** som iOS.

## Schweiss-registret

Ikke en fane. **Anskudt**-knappen står i toppen af Jagtbart og åbner et `ModalBottomSheet`. Indholdet (`SchweissContent`) genbruges i Indstillinger → Om, hvor det skubbes ind som skærm. To veje, begge synlige, ingen omskifter: Anskudt under jagt (link til schweiss.dk og Naturstyrelsen) og påkørt hjortevildt (Ring 1812). Telefonnumre åbner opkalds-appen via `ACTION_DIAL`, mail åbner mail-appen, nettet åbner browseren. Seks dagtimer er tekst med kilde. Ingen timer. Ingen telefonliste, ingen scrape. Appen er ikke loven.

## Journal

Anden fane i bundnavigationen (`NavigationBar`): Jagtbart, så Journal. Dato og sted har egen kopi her (`BagDraft`) — de ændrer ikke Jagtbart. Swipe til venstre på en artsrække i Jagtbart noterer ét stykke og skifter til Journal; rækken springer tilbage (`SwipeToDismissBox` med `confirmValueChange` der returnerer falsk — handlingen er en notering, ikke en sletning). Bekræftelsen står nederst: “[Art] noteret”, Fortryd, +1, Se året. Årets liste grupperer på måned, og swipe sletter for alvor dér. Artslisten kan tilføjes fra et ark med søgning. Ingen iCloud: journalen er en lokal fil med rullende backup (`vildtudbytte.json`, `artsliste.json`), skrevet atomisk med bevaring af en ødelagt fil. Android Auto Backup er slået til med eksplicitte regler (journal og præferencer med, katalog-cachen og `*.corrupt.json` fra), så journalen kan ligge i brugerens Android-backup og følge med til en ny telefon — som iOS' iCloud, bare via Google. Slet en post: swipe eller **Slet** i Ret viser “Slettet” med **Fortryd** i bunden. Det er kun Android; iOS har ingen fortryd ved sletning. Ret → Slet og Tilbage fra Ret går til årslisten.

## Alle arter

Tredje fane. Generelle jagttider for alle arter, grupperet i kort der starter sammenfoldet ("Sammenfold grupper" gælder også her). Søgning folder grupperne med træf ud uden ekstra tryk. Arterne er slået op ved Danmark, så ingen lokal tid lækker ind. Ingen dato, intet sted, ingen åben/lukket. Rækkerne ligger inde i gruppekortene (`ui/SpeciesRow.kt`, `ui/GroupHeader.kt`, delt med Jagtbart) og har samme journal-swipe som Jagtbart. Detaljesiden viser den generelle kontekst: ingen status, tynd SeasonBar uden markør og artnoten i stedet for stedens note.

## Indstillinger

Fjerde fane (tandhjul). Øverst **Udseende** som en `SingleChoiceSegmentedButtonRow` (System/Lys/Mørk med ikoner) — valget ligger i "udseende"-præferencen og styrer `JagtFokusTheme`, så hele appen skifter med det samme. Så **Lister** med "Sammenfold grupper". Derefter Om-sektionerne i samme liste, som iOS: Data (kilde, opdateret, bekendtgørelsen), Ansvar, Schweiss-registret (skubbet ind som skærm, ikke ark), Fotos, Privatliv og Denne app med Version (fra PackageManager) og Dataversion (fra kataloget).

## Velkomst

Første start. Mørkegrøn brandverden (`BrandDeep` med glow fra øverst højre), mærket, tre punkter i guld (dato og sted, lokale jagttider, privat og offline), fraskrivelsen i en boks med guldkant og knappen **Forstået** i guld. Den forklarer positionsbrugen, før systemets dialog kommer. Gemt i "velkomst"-præferencen; slettes data, kommer den igen.

## Sprog og ikon

Appen følger systemets appsprog (dansk eller engelsk; ukendt falder tilbage til dansk). Datoer og tal formateres med sproget fra `locale_tag`-strengen, men dagene regnes i Europe/Copenhagen. Engelsk følger iOS' engelske tekster ordret.

Launcher-ikonet har et enfarvet lag (`ic_launcher_monochrome`, afledt af mærket) til temaikoner. Release-builds bruger R8.

## Endnu ikke bygget

Intet. iOS-appens faner og velkomst findes alle i Android-delen.
