# AppleTV Launcher (Android TV / Google TV)

Launcher natif Kotlin inspiré de tvOS, pour la TCL 43P635 (Google TV).
Projet indépendant, non affilié à Apple. Aucun logo Apple n'est utilisé.

## Fonctions

- Interface 16:9 proportionnelle : identique en Full HD et en 4K.
- Grand espace supérieur : affiches de films/séries (optionnel, via TMDB) ou bannière de l'appli sélectionnée.
- Rangée d'applications **réellement installées** (détection automatique, mise à jour à l'installation/désinstallation).
- Agrandissement fluide de l'icône sélectionnée, reflet, ombre, fond d'ambiance teinté par l'appli.
- Télécommande : ← → naviguer, **OK** ouvrir, **maintenir OK** infos de l'appli, **Retour** revient au premier élément puis quitte.
- Visible dans la liste des applis Google TV (`LEANBACK_LAUNCHER`). **Ne remplace pas l'accueil** : le bouton Accueil de la télécommande ramène à Google TV.

## Compiler sans PC (GitHub Actions)

1. Créez un dépôt GitHub et envoyez-y **tout le contenu** de ce dossier (y compris `.github/` et `gradle/`).
2. Onglet **Actions** → workflow **Build debug APK** (il se lance à chaque push, ou via *Run workflow*).
3. Récupérez l'APK :
   - dans l'artefact **AppleTVLauncher-debug-apk** du run, ou
   - dans **Releases → AppleTV Launcher (dernier build debug)** (lien direct, pratique depuis la TV).

## Installer sur la TCL

1. Paramètres → Système → À propos : appuyez 7 fois sur *Build* pour activer le mode développeur, puis autorisez l'installation d'applications inconnues pour l'appli qui télécharge.
2. Installez l'appli **Downloader** (Play Store) et saisissez l'URL de l'APK de la release, ou utilisez `adb install AppleTVLauncher-debug.apk` en Wi-Fi.
3. Lancez **AppleTV Launcher** depuis la rangée « Applications » de Google TV.

## Affiches de films et séries (optionnel)

1. Créez une clé API gratuite sur themoviedb.org.
2. Dans GitHub : Settings → Secrets and variables → Actions → *New repository secret* nommé `TMDB_API_KEY`.
3. Relancez le workflow. Sans clé, l'espace supérieur affiche la bannière de l'appli sélectionnée.

Ce produit utilise l'API TMDB mais n'est ni approuvé ni certifié par TMDB.

## Devenir plus tard l'écran d'accueil

Un bloc commenté dans `app/src/main/AndroidManifest.xml` (catégories `HOME` + `DEFAULT`) permettra de proposer le launcher comme accueil. Dans ce cas, adaptez le comportement de **Retour** dans `MainActivity` (il appelle `finish()` quand la sélection est déjà sur le premier élément).

## Structure

- `MainActivity` : interface, télécommande, rotation des affiches.
- `AppRepository` : détection des applis, bannières/icônes, couleur d'ambiance (avec cache).
- `AppAdapter` / `AppCardView` : rangée leanback `HorizontalGridView` et animations de focus.
- `HeroView` / `CrossfadeImageView` : espace supérieur, fondus et dégradés.
- `FeaturedRepository` / `ImageLoader` : tendances TMDB et chargement d'images avec cache mémoire.
