#!/bin/bash

read -p "Please enter the Stackname to be terminated: " stackname

statuscheck=$(aws cloudformation describe-stacks --stack-name $stackname --query "Stacks[0].StackStatus" >/dev/null 2>&1)
 
if [ $? -eq 0 ]
then
    aws cloudformation delete-stack --stack-name $stackname
    echo "terminating stack...."
    while true; do
        aws cloudformation describe-stacks --stack-name $stackname --query "Stacks[0].StackStatus" >/dev/null 2>&1
        if [ $? -ne 0 ]; then
            break
        fi
        echo -n "."
    done
    echo "."
    echo "Successfully terminated stack: $stackname"
else
    echo "Unable to delete stack as there exists no stack with name: $stackname"
fi













































