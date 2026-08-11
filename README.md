# MCPC+ Java 8 Agent

Lance un serveur **MCPC+ / Cauldron 1.6.4** sous **Java 8**, sans modifier le jar du serveur ni les mods.

Ces serveurs refusent de démarrer sous Java 8, ou plantent pendant le chargement de Forge. Cet agent lève les trois blocages au démarrage.

## Installation

1. Récupérez `mcpc-java8-agent.jar` depuis les [releases](../../releases), ou compilez-le (voir plus bas).
2. Placez-le où vous voulez, par exemple à côté du jar du serveur.
3. Ajoutez `-javaagent:` à votre ligne de lancement, **avant** `-jar` :

```sh
java -javaagent:mcpc-java8-agent.jar -Xmx4G -jar mcpc.jar nogui
```

C'est tout. Aucun fichier de configuration, aucune modification du serveur ou des mods.

### Exemple de start.sh

```sh
#!/bin/sh
java -javaagent:mcpc-java8-agent.jar \
     -Xms2G -Xmx4G \
     -jar mcpc.jar nogui
```

### Vérifier que ça marche

Au démarrage, la console affiche :

```
[mcpc-j8] Java 8 detected (1.8.0_412), reporting 1.7.0_80 to bypass the MCPC+ version check.
[mcpc-j8] 4 class patch(es) loaded.
[mcpc-j8] Patched: net/minecraft/launchwrapper/Launch
```

Si vous ne voyez aucune ligne `[mcpc-j8]`, l'agent n'est pas chargé : vérifiez que `-javaagent:` est bien **avant** `-jar` et que le chemin du jar est correct.

## Ce que l'agent corrige

**1. Le refus explicite de Java 8.** `FMLLaunchHandler` teste `java.version` et arrête le serveur avec le message `Java 8 is not supported on MCPC+8.11! Please run MCPC+ using Java 7.`

L'agent présente une version `1.7.x` à cette vérification. Passer `-Djava.version=1.7.0_80` sur la ligne de commande **ne fonctionne pas** : la JVM réécrit cette propriété pendant son initialisation. Il faut le faire depuis un agent, qui s'exécute après l'initialisation de la JVM mais avant le `main` du serveur.

Un coremod ne peut pas corriger ce point : `FMLLaunchHandler` est explicitement exclu du `LaunchClassLoader`, donc hors de portée de tout `IClassTransformer`.

**2. Un plantage dans launchwrapper.** `Launch.launch` parcourt la liste des tweakers tout en la modifiant, ce qui lève une `ConcurrentModificationException` sous Java 8.

Si votre serveur utilise déjà launchwrapper 1.11 ou supérieur, ce point est déjà réglé de votre côté ; l'agent reste inoffensif.

**3. Des plantages du déobfuscateur de Forge.** La version d'ASM embarquée dans ces serveurs échoue sur du bytecode produit par Java 8 (`Remapper`, `ClassReader`, `MethodInsnNode`).

## Portée et limites

L'agent débloque le **lancement**. Il ne rend pas compatibles des mods qui dépendraient d'API supprimées en Java 8 — un cas rare sur cette génération de mods.

Il ne touche à rien d'autre : pas de modification du monde, des mods, ni du comportement du serveur. Si un correctif ne peut pas s'appliquer, la classe d'origine est laissée intacte et le démarrage continue.

Testé sur MCPC+ 8.11 / Cauldron 1.6.4 avec OpenJDK 8 et Zulu 8, en production.

## Compiler

```sh
./gradlew jar
```

Le jar est produit dans `build/libs/mcpc-java8-agent.jar`. Aucune dépendance externe.

## Contenu

Les fichiers `.bin` sont les classes d'origine de **launchwrapper** et **ASM**, recompilées avec la correction minimale nécessaire. Elles restent sous la licence de leurs projets respectifs : MIT pour launchwrapper, BSD 3-clauses pour ASM.

Le code de l'agent lui-même est publié sous licence MIT.
