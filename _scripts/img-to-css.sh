#!/bin/bash
echo "" > $2
for i in `ls ../img/*$1.png` ; do
	echo -n ".$(basename ${i%$1.png}) {
	width:$(identify $i | awk '{print $3}' | awk -Fx '{print $1}')px;
	height:$(identify $i | awk '{print $3}' | awk -Fx '{print $2}')px;
	background-repeat: no-repeat;	
	background-image: url(data:image/png;base64," >> $2
	../img/_base64.sh $i >> $2
	echo -n ");
}
" >> $2
done
