# MCPC+ Java 17 Agent

Fait tourner un serveur **MCPC+ / Cauldron 1.6.4** sous **Java 17**, sans modifier le jar du serveur ni les mods.

## Installation

### 1. Installer Java 17

Utilisez **Eclipse Temurin 17** (gratuit, c'est le JDK avec lequel l'agent est testé) :

👉 **https://adoptium.net/temurin/releases/?version=17**

Choisissez votre système (Windows, Linux, macOS), le type **JDK** et téléchargez l'installeur (`.msi` sous Windows, `.pkg` sous macOS, `.tar.gz` ou paquet sous Linux).

- **Debian / Ubuntu** : suivez https://adoptium.net/installation/linux/ puis `sudo apt install temurin-17-jdk`
- **Pterodactyl / hébergeur** : sélectionnez l'image ou la version **Java 17** dans les paramètres du serveur.

Vérifiez l'installation :

```sh
java -version
```

La réponse doit contenir `17.` (par exemple `openjdk version "17.0.12"`).

### 2. Télécharger l'agent

Téléchargez **`mcpc-java17-agent.jar`** depuis la [dernière release](../../releases/latest) et placez-le dans le dossier du serveur, à côté de `mcpc.jar`.

### 3. Modifier le script de lancement

Ajoutez `-javaagent:mcpc-java17-agent.jar` **avant** `-jar` :

**Linux (`start.sh`)**

```sh
#!/bin/sh
java -javaagent:mcpc-java17-agent.jar -Xms2G -Xmx4G -jar mcpc.jar nogui
```

**Windows (`start.bat`)**

```bat
java -javaagent:mcpc-java17-agent.jar -Xms2G -Xmx4G -jar mcpc.jar nogui
pause
```

**Pterodactyl** : ajoutez `-javaagent:mcpc-java17-agent.jar` dans la commande de démarrage, juste avant `-jar`.

Remplacez `mcpc.jar` par le nom de votre jar serveur.

> ⚠️ **Retirez `-XX:+UseConcMarkSweepGC`** et toutes les options `-XX:CMS...` si votre ancien script en contient : elles n'existent plus en Java 17 et **empêchent le serveur de démarrer**. Les options `-XX:PermSize` / `-XX:MaxPermSize` peuvent aussi être retirées.

C'est tout : rien d'autre à modifier, ni dans le serveur, ni dans les mods, ni dans `libraries/`.

### 4. Vérifier

Au démarrage, la console doit afficher des lignes `[mcpc-j17]` :

```
[mcpc-j17] Java 17.0.12 detected, reporting 1.7.0_80 to bypass the MCPC+ version check.
[mcpc-j17] Opened 741 JDK packages to the server.
...
[mcpc-j17] ASM 4 detected, upgrading it to the bundled ASM.
```

puis le serveur démarre normalement (`Done (...)! For help, type "help"`).

## Problèmes fréquents

| Symptôme | Solution |
|---|---|
| Aucune ligne `[mcpc-j17]` | L'agent n'est pas chargé : `-javaagent:` doit être **avant** `-jar`, et le nom du fichier doit être exact. |
| `Unrecognized VM option 'UseConcMarkSweepGC'` | Retirez les options `CMS` du script (voir étape 3). |
| `UnsupportedClassVersionError` au lancement | Vous n'êtes pas en Java 17 : vérifiez `java -version`, ou indiquez le chemin complet de Java 17 dans le script. |
| `Java 8 is not supported on MCPC+...` | L'agent n'est pas chargé (voir la première ligne). |
| `Block ID ... is outside the supported range` | Un mod utilise un ID très élevé : ajoutez `-Dmcpcj17.idCapacity=1048576` (ou plus) avant `-jar`. |

Si le serveur plante malgré tout, ouvrez une issue avec le log complet (`ForgeModLoader-server-0.log`).

## Ce que fait l'agent

Au démarrage, avant le serveur :

- fait passer le test de version de MCPC+ (qui exige Java 7) ;
- corrige launchwrapper et le chargement des classes du JDK pour Java 9+ ;
- ouvre au serveur les API internes du JDK, dont Forge, Bukkit et les mods se servent ;
- remplace `sun.reflect`, supprimé depuis Java 9 (ajout de biomes, matériaux... par Forge) ;
- remplace ASM 4 par **ASM 9.7** : les mods compilés en Java 8+ et les coremods récents se chargent ;
- accepte les mods qui utilisent des IDs au-delà des limites vanilla (4096 blocs, 32000 items), jusqu'à 65536 par défaut.

> **Blocs au-delà de 4095 :** MCPC+ ne sait pas les enregistrer dans le monde. Ils sont chargés comme **blocs factices** : le mod fonctionne, mais ces blocs ne peuvent pas être posés (ils sont remplacés par de l'air, et la console l'indique). Les blocs et items sous ces limites ne sont pas concernés.

L'agent ne modifie ni le monde, ni les mods, ni les fichiers du serveur. Il ne rend pas compatibles les mods ou plugins qui utilisent des fonctions retirées du JDK (moteur JavaScript Nashorn, `javax.xml.bind`...).

**Testé** avec Java 17 sur MCPC+ 1.6.4 (Forge 9.11.1.965) et sur le build NGServer, avec un mod Java 8 et un coremod ASM 9.

## Compiler

```sh
./gradlew jar
```

Nécessite un JDK 17. Le jar est produit dans `build/libs/mcpc-java17-agent.jar`. La CI GitHub construit le jar à chaque push ; un tag `vX.Y.Z`, ou le workflow *Build* lancé à la main avec un tag, publie une release.

## Licences

Code de l'agent : MIT. Le jar embarque **ASM** (BSD 3-clauses), adapté au build pour rester compatible avec Forge 1.6.4 (voir `build.gradle`), et une classe `Launch` de **launchwrapper** (MIT) corrigée.
