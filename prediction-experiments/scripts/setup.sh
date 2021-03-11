wget https://repo.anaconda.com/archive/Anaconda3-2019.10-Linux-x86_64.sh
conda create -n hbcp python=3.6
source activate hbcp
pip install -r prediction/requirements.txt
#conda install -c anaconda keras
pip install -I tensorflow
pip install -I keras
