#!/bin/bash
#sh scripts/run.sh
PROJECT=.
TOOL_PATH=$PROJECT/data/TempEval3/Tools/tools
NORMALIZER=$TOOL_PATH/TimeML-Normalizer/TimeML-Normalizer.jar
EVALUATOR=$TOOL_PATH/evaluation-relations/temporal_evaluation.py
#GOLD_PATH=$PROJECT/output/dev/gold_dev
GOLD_PATH=./data/TempEval3/Evaluation/te3-platinum
SYS_PATH=$PROJECT/output/pred
GOLD_NORM_PATH=$GOLD_PATH'-normalized'
SYS_NORM_PATH=$SYS_PATH'-normalized'

LOGFILE=$PROJECT/logs/evaluation_process_$1.txt
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

