#!/bin/bash
#sh scripts/run.sh

#args={gold_path,sys_path,tag,dir}

PROJECT=.
TOOL_PATH=$PROJECT/data/TempEval3/Tools/tools
NORMALIZER=$TOOL_PATH/TimeML-Normalizer/TimeML-Normalizer.jar
EVALUATOR=$TOOL_PATH/evaluation-relations/temporal_evaluation.py
GOLD_PATH=$1
SYS_PATH=$2
GOLD_NORM_PATH=$GOLD_PATH'-normalized'
SYS_NORM_PATH=$SYS_PATH'-normalized'

LOGFILE=$PROJECT/logs/$4/evaluation_process_$3.txt
echo NORMALIZATION BEGINS':\n' > $LOGFILE
# normalization
for file in `ls $GOLD_PATH`; do
	echo "Normalizing $file...":'\n' >> $LOGFILE
	CMD="java -jar $NORMALIZER -d -a \"$GOLD_PATH/$file;$SYS_PATH/$file\""
	echo $CMD >> $LOGFILE
	echo '\n' >> $LOGFILE
	${CMD}
done
# evaluation
echo EVALUATION BEGINS':\n' >> $LOGFILE
CMD="python $EVALUATOR $GOLD_NORM_PATH $SYS_NORM_PATH 3"
echo $CMD >> $LOGFILE
${CMD} >> $LOGFILE

