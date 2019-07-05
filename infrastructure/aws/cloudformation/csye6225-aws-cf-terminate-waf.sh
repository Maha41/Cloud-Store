#!/bin/bash -e


RC=$(aws cloudformation describe-stacks --stack-name waf --query Stacks[0].StackId --output text)

if [ $? -eq 0 ]
then
	continue
else
	echo "Stack $1 doesn't exist..."
	exit 0
fi


RC=$(aws cloudformation delete-stack --stack-name waf)
echo "Deletion in progress..."
RC=$(aws cloudformation wait stack-delete-complete --stack-name waf)

if [ $? -eq 0 ]
then
  echo "WAF stack deletion complete successfully"
else
 	echo "Failed Stack deletion"
 	exit 1
fi