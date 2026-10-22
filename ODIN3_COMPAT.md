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
| Power Stone (Europe) | 🔵 Jouable | Combat arcade (Falcon contre Wang-Tang, 30 s), démo d'attraction, menus | Vitesse 100 % (chrono du combat : 10,00 s de jeu par 10 s), 46-49 images/s affichées pour 50 (jeu PAL, écran 120 Hz), 12/s au chargement de la démo | Vulkan, rendu 1920 lignes, rendu threadé, réglages copiés de l'app officielle ; ~3 images/s jamais affichées (cause à chercher) ; banc odin3-lab validé (même démo et mêmes menus VM/Odin) | 2026-10-22 | a4c34163 |
