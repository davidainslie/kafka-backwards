#!/usr/bin/env bash

minikube start

minikube addons enable dashboard

minikube addons enable ingress

sleep 60s

kubectl create -f deployment.yaml

kubectl create -f service.yaml

sleep 60s

kubectl get pods

minikube dashboard