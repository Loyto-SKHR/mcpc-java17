# MCPC+ Java 17 Agent

Lance un serveur **MCPC+ / Cauldron 1.6.4** sous **Java 17**, sans modifier le jar du serveur ni les mods.

Ces serveurs refusent de démarrer sur autre chose que Java 7, puis plantent pendant le chargement de launchwrapper et de Forge, car ils s'appuient sur des comportements de Java 7/8 qui n'existent plus en Java 17. Cet agent lève ces blocages au démarrage.

## Installation

1. Récupérez `mcpc-java17-agent.jar` depuis les [releases](../../releases), ou compilez-le (voir plus bas).
2. Placez-le où vous voulez, par exemple à côté du jar du serveur.
3. Ajoutez `-javaagent:` à votre ligne de lancement, **avant** `-jar` :

```sh
java -javaagent:mcpc-java17-agent.jar -Xmx4G -jar mcpc.jar nogui
```

C'est tout. Aucun `--add-opens`, aucun fichier de configuration, aucune modification du serveur ou des mods.

### Exemple de start.sh

```sh
#!/bin/sh
java -javaagent:mcpc-java17-agent.jar \
     -Xms2G -Xmx4G \
     -jar mcpc.jar nogui
```

### Options JVM à retirer

Certaines options courantes des anciens scripts de lancement n'existent plus en Java 17 :

- `-XX:+UseConcMarkSweepGC` et les options `CMS*` : supprimées, **la JVM refuse de démarrer**. Retirez-les (G1, le GC par défaut, convient très bien).
- `-XX:PermSize` / `-XX:MaxPermSize` : ignorées depuis Java 8, vous pouvez les retirer.

### Vérifier que ça marche

Au démarrage, la console affiche :

```
[mcpc-j17] Java 17.0.x detected, reporting 1.7.0_80 to bypass the MCPC+ version check.
[mcpc-j17] Opened 741 JDK packages to the server.
[mcpc-j17] Reflection filters removed.
[mcpc-j17] 4 class patch(es) loaded.
[mcpc-j17] Patched: net/minecraft/launchwrapper/Launch
[mcpc-j17] LaunchClassLoader delegates 241 platform packages to the JDK.
[mcpc-j17] Redirected sun.reflect in: net/minecraftforge/common/EnumHelper
```

Si vous ne voyez aucune ligne `[mcpc-j17]`, l'agent n'est pas chargé : vérifiez que `-javaagent:` est bien **avant** `-jar` et que le chemin du jar est correct.

## Ce que l'agent corrige

**1. Le refus de toute version autre que Java 7.** `FMLLaunchHandler` teste `java.version` et arrête le serveur (`... is not supported on MCPC+8.11! Please run MCPC+ using Java 7.`).

L'agent présente une version `1.7.x` à cette vérification. Passer `-Djava.version=1.7.0_80` sur la ligne de commande **ne fonctionne pas** : la JVM réécrit cette propriété pendant son initialisation. Il faut le faire depuis un agent, qui s'exécute après l'initialisation de la JVM mais avant le `main` du serveur. Un coremod ne peut pas corriger ce point : `FMLLaunchHandler` est exclu du `LaunchClassLoader`.

**2. Le démarrage de launchwrapper.** `Launch` construit son class loader à partir de `(URLClassLoader) getClass().getClassLoader()`. Depuis Java 9, le class loader de l'application n'est plus un `URLClassLoader` : `ClassCastException` immédiate. La version corrigée de `Launch` lit le class path depuis `java.class.path`, comme les versions récentes de launchwrapper.

La version corrigée garde aussi le correctif de la `ConcurrentModificationException` dans `Launch.launch` (liste des tweakers modifiée pendant son parcours).

**3. Les classes du JDK hors de `java.base`.** Le `LaunchClassLoader` a pour parent le class loader de bootstrap. En Java 8, celui-ci voyait tout le JDK ; depuis Java 9, des modules comme `java.sql` ou `java.scripting` sont chargés par le *platform class loader*, et des classes comme `javax.sql.DataSource` (Ebean, plugins Bukkit) deviennent introuvables. L'agent fait déléguer ces packages au JDK.

**4. L'encapsulation des modules du JDK.** Forge, Bukkit et les mods appellent `setAccessible` sur des classes internes du JDK, ce que Java 17 refuse par défaut (`InaccessibleObjectException`). L'agent ouvre tous les packages du JDK au serveur, l'équivalent d'une longue liste de `--add-opens ...=ALL-UNNAMED`. Il retire aussi les filtres de réflexion ajoutés en Java 12 (qui cachent notamment `Field.modifiers`).

**5. `sun.reflect.ReflectionFactory`, supprimé en Java 9.** L'`EnumHelper` de Forge s'en sert pour ajouter des valeurs aux enums (biomes, types de créatures, matériaux Bukkit de MCPC+...). Les références à `sun.reflect.ReflectionFactory`, `ConstructorAccessor` et `FieldAccessor` sont redirigées, dans toutes les classes chargées, vers des équivalents fournis par l'agent qui fonctionnent sous Java 17.

**6. Le déobfuscateur de Forge.** La version d'ASM embarquée dans ces serveurs échoue sur du bytecode récent (`Remapper`, `ClassReader`, `MethodInsnNode`). Les versions corrigées acceptent les classes jusqu'à Java 17.

## Portée et limites

L'agent débloque le **lancement**. Il ne rend pas compatibles des mods ou plugins qui dépendraient d'API supprimées du JDK (moteur JavaScript Nashorn, `javax.xml.bind`, `Thread.stop`...) ; ces cas restent à traiter au cas par cas.

Il ne touche à rien d'autre : pas de modification du monde, des mods, ni du comportement du serveur. Si un correctif ne peut pas s'appliquer, la classe d'origine est laissée intacte et le démarrage continue.

L'agent requiert Java 17 ou plus récent ; il ne se charge pas sous Java 8.

## Compiler

```sh
./gradlew jar
```

Nécessite un JDK 17 ou plus récent. Le jar est produit dans `build/libs/mcpc-java17-agent.jar`. Aucune dépendance externe.

## Contenu

Les fichiers `.bin` sont les classes d'origine de **launchwrapper** et **ASM**, recompilées avec la correction minimale nécessaire (pour `Launch`, le constructeur appelle `gg.nationsglory.mcpcj17.LaunchHooks`). Elles restent sous la licence de leurs projets respectifs : MIT pour launchwrapper, BSD 3-clauses pour ASM.

Le code de l'agent lui-même est publié sous licence MIT.
