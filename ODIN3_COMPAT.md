# Compatibilité Odin 3 : flycast (Dreamcast/Naomi)

Notre propre liste de compatibilité, tenue d'après nos tests sur l'AYN Odin 3 (Snapdragon 8 Elite, Adreno 830) avec notre build de la branche `odin3`. Chaque test met à jour la ligne du jeu : statut, mesure, date et commit testé. Amont : https://github.com/flyinghead/flycast

## Échelle

| Statut | Sens |
|---|---|
| 🟢 Parfait | vu en partie, plein régime, aucun défaut vu |
| 🔵 Jouable | vu en partie, défauts mineurs |
| 🟡 Problèmes | vu en partie, défauts gênants (fps, graphismes, son) |
| 🟠 Menus/intro | démarre, pas encore vu en partie |
| 🔴 Ne marche pas | crash, écran noir ou blocage |
| ⚪ Non testé | présent sur la console, jamais lancé |

## Comment on teste

Labo commun `odin3-lab` (VM ARM64 pages 4 Ko + profil Vulkan de l'Adreno 830) pour le CPU et la logique, puis l'Odin 3 par adb pour le GPU, les fps et la chauffe.

## Jeux

| Jeu | Statut | Vu jusqu'à | Mesure | Notes | Testé le | Build |
|---|---|---|---|---|---|---|
| Power Stone (Europe) | 🔵 Jouable | Combat arcade (Falcon contre Wang-Tang, 30 s), démo d'attraction, menus | Vitesse 100 % (chrono du combat : 10,00 s de jeu par 10 s), 46-49 images/s affichées pour 50 (jeu PAL, écran 120 Hz), 12/s au chargement de la démo | Vulkan, rendu 1920 lignes, rendu threadé, réglages copiés de l'app officielle ; ~3 images/s jamais affichées (cause à chercher) ; banc odin3-lab validé (même démo et mêmes menus VM/Odin) ; changement de jeu depuis le frontend (Power Stone, Home, Sonic Adventure, Home, Power Stone) : chaque autre jeu démarre dans un processus neuf, le même jeu reprend là où il était (2cf99058, 26/10) | 2026-10-26 | 2cf99058 |
| Power Stone 2 (Europe) | 🟠 Menus/intro | Écran de choix 50/60 Hz | Joueurs façon Switch vérifiés (logcat « plays as player N ») : manette externe seule J1 et pilote le menu, 2e manette (dongle Xbox simulé) J2, Odin en plus J3 ; Odin seule J1, curseur déplacé à la croix | Les manettes rejoignent à l'appui, ports d'emu.cfg ignorés, J1 rendu à l'Odin à la sortie du dock ou au départ de sa manette | 2026-10-26 | a8abfd91 + ce commit |
