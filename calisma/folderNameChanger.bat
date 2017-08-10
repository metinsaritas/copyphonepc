set dizin=git
IF EXIST %dizin% (
cls
	echo "bulundu: %dizin%"
	rename %dizin% .%dizin%
) ELSE IF EXIST .%dizin% (
cls
	echo "bulundu: .%dizin%"
	rename .%dizin% %dizin%
) ELSE (echo "Not found")
pause