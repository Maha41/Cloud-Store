#!/bin/bash -e

#STACKPREFIX="WAF"
#STACKSCOPE="Regional"
#RULEACTION="BLOCK"
#INCLUDESPREFIX="/includes"
#ADMINURLPREFIX="/admin"
#ADMINREMOTECIDR="127.0.0.1/32"
#MAXEXPECTEDURISIZE=512
#MAXEXPECTEDQUERY=1024
#MAXEXPECTEDBODY=4096
#MAXEXPECTEDCOOKIESIZE=4093
#CSRFEXPECTEDHEADER="x-csrf-token"
#CSRFEXPECTEDSIZE=36
LOADBALANCER=$(aws elbv2 describe-load-balancers --query LoadBalancers[0].LoadBalancerArn --output text)

echo "ELBResourceARN: $LOADBALANCER"

echo "Starting waf creation"
aws cloudformation create-stack --stack-name waf --template-body file://./owasp_10_base.yml --parameters ParameterKey=LOADBALANCER,ParameterValue=$LOADBALANCER --output text
aws cloudformation wait stack-create-complete --stack-name waf --output text
STACKDETAILS=$(aws cloudformation describe-stacks --stack-name $1-waf --query Stacks[0].StackId --output text)

