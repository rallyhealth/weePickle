rm -rf .idea*
mill mill.scalalib.GenIdea/idea
# The generated modules use overlapping directories which will confuse IntelliJ. Delete them.
# https://github.com/lihaoyi/mill/issues/478 would help here.
#
# IntelliJ uTest runner only works with <= 2.12 otherwise you get:
# ClassNotFoundException for com.rallyhealth.weepickle.v1.JsonTests: com.rallyhealth.weepickle.v1.JsonTests
echo "Removing:"
rm -fv .idea_modules/*.js-* .idea_modules/*.js.* .idea_modules/*-2.11* .idea_modules/*-2.13*

sed -e '/.*\.js-.*/d' -i '' .idea/modules.xml
sed -e '/.*\.js.iml*/d' -i '' .idea/modules.xml
sed -e '/.*-2.11.*/d' -i '' .idea/modules.xml
sed -e '/.*-2.13.*/d' -i '' .idea/modules.xml

# Hack play-json cross building.
perl -pe 's|(?<!out)/weejson/play/2.12.8|/weejson/play|g' -i.bak .idea_modules/weejson.play*
rm .idea_modules/*.bak

echo
echo "IntelliJ project ready. You can open IntelliJ now."

echo "Running source generators since IntelliJ won't do that for you..."
set -x
mill __.generatedSources
