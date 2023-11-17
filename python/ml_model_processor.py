from pathlib import Path
from zkml import LeoTranspiler
import pickle
import sys

in_file = sys.argv[1]
project_path = sys.argv[2]
leo_project_name = sys.argv[3]

with open(in_file, 'rb') as handle:
  clf = pickle.load(handle)
  lt = LeoTranspiler(model=clf)
  lt.to_leo(
      path=Path(project_path), project_name=leo_project_name, fixed_point_scaling_factor=16
  )
