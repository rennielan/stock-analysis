import React from 'react';
import { Star } from 'lucide-react';

interface StarRatingProps {
  value: number;
  onChange: (val: number) => void;
}

const StarRating: React.FC<StarRatingProps> = ({ value, onChange }) => {
  return (
    <div className="flex gap-1">
      {[1, 2, 3, 4, 5].map((star) => (
        <button
          key={star}
          type="button"
          onClick={() => onChange(star)}
          className="focus:outline-none transition-transform active:scale-95"
        >
          <Star
            size={16}
            className={`${
              star <= value
                ? 'fill-yellow-500 text-yellow-500'
                : 'fill-transparent text-slate-600 hover:text-slate-500'
            } transition-colors`}
          />
        </button>
      ))}
    </div>
  );
};

export default StarRating;