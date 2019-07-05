#!/bin/bash

echo "Starting with Stack creation process......"
echo "Validating the Template......"

aws cloudformation validate-template --template-body file://csye6225-cf-lambda.json #>/dev/null 2>&1

if [ $? -eq 0 ]
then
    echo "Template validation successful"
    read -p "please enter the Stackname: " stackname
    echo "creating stack $stackname now ......"
    aws cloudformation create-stack --stack-name $stackname --template-body file://csye6225-cf-lambda.json --capabilities CAPABILITY_NAMED_IAM
    if [ $? -eq 0 ]
    then
        while true;
        do
            completionCheck=$(aws cloudformation describe-stacks --stack-name $stackname --query "Stacks[0].StackStatus")
            if [ $completionCheck = "\"CREATE_COMPLETE\"" ]
            then
                break
            else
                echo -n "."
            fi
        done
        echo "."
        echo "stack $stackname created Successfully"
    else
        errorCheck=$(aws cloudformation describe-stacks --stack-name $stackname --query "Stacks[0].StackStatus")
        if [ $errorCheck = "\"CREATE_COMPLETE\"" ]
        then
            echo "Stack: $stackname already exists"
        else
            echo "unable to create stack $stackname"
        fi
    fi
else
    echo "Unable to validate template"
fi
