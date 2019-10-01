rm -rf .idea*
mill mill.scalalib.GenIdea/idea
# The generated modules use overlapping directories which will confuse IntelliJ. Delete them.
# IntelliJ uTest runner only works with <= 2.12 so that's the one we keep here.
rm -fv .idea_modules/*.js-* .idea_modules/*-2.11* .idea_modules/*-2.13*
sed -e '/.*\.js-.*/d' -i '' .idea/modules.xml
sed -e '/.*-2.11.*/d' -i '' .idea/modules.xml
sed -e '/.*-2.13.*/d' -i '' .idea/modules.xml
