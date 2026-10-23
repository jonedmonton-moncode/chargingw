# Charging W
Widget Android transparent 1×1. Affiche la puissance estimée à la batterie en W, ou « – » si débranché. Réglage de la fréquence en secondes dans l'application.

## Compiler sans ordinateur
Déposer le contenu de ce projet dans un dépôt GitHub (branche main). Ouvrir Actions > Build APK > Run workflow, puis télécharger l'artefact ChargingW-APK après la compilation. Installer app-debug.apk sur le téléphone.

## Limites
La puissance provient du courant de batterie et de sa tension, et non directement du chargeur. Certaines ROM renvoient des valeurs indisponibles ou de signe différent. Une notification persistante est utilisée pour le service de rafraîchissement. Les restrictions batterie MIUI peuvent arrêter ce service. APK debug signé automatiquement pour test personnel.
