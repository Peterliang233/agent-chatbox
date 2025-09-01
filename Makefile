msg ?= "init"
debug:
	echo "输入的信息是:$(msg)"
commit:
	git add .
	git commit -m "$(msg)"
	git push origin main