Runtime patch after first successful server start.

What was wrong

1. The root runServer tasks were passing the spell-pack jar to Paper as a normal plugin jar.
   That cannot work because the spell-pack jar does not contain plugin.yml or paper-plugin.yml.
   It is content for mageswand to load from plugins/mageswand/spells/.

2. FuelRegistry only accepted this JSON shape:
   {
     "mappings": [ ... ]
   }

   But the example config and the generated selected config both used a top-level array:
   [
     { ... }
   ]

What changed

- runServer now stages the full spell-pack jar into run/plugins/mageswand/spells/
- runServerSelected now stages the selected spell-pack jar into run-selected/plugins/mageswand/spells/
- runServerCoreOnly clears any staged spell-pack jars from run-core-only
- FuelRegistry now accepts either:
  - a top-level array of mappings
  - an object with a mappings array
- core-only config generation now writes an explicit object form with an empty mappings array
