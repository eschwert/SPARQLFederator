package br.org.accamargo.cipe.gqe;


import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.algebra.TransformBase;
import com.hp.hpl.jena.sparql.algebra.TransformCopy;
import com.hp.hpl.jena.sparql.algebra.op.OpJoin;
import com.hp.hpl.jena.sparql.algebra.op.OpService;
import com.hp.hpl.jena.sparql.algebra.op.OpUnion;
import com.hp.hpl.jena.sparql.algebra.optimize.TransformDistinctToReduced;

public class GrumpyOptimizer extends TransformCopy {



    private int compareService( OpService left, OpService right ) {
        return left.getService().toString().compareToIgnoreCase(right.getService().toString());
    }
	
	@Override
	public Op transform(OpJoin opJoin, Op left, Op right) {
		
		// doppelganger #1
		// regra : JOIN( S1( x ), S1( y ) ) => S1( JOIN( x, y ) )
		if ( ( left instanceof OpService && right instanceof OpService) ) {
			
			Node leftServiceNode = ((OpService)left).getService();
			Node rightServiceNode = ((OpService)right).getService();
	
			// merge
            if ( compareService( (OpService)left, (OpService)right ) == 0 ) {
				return new OpService(
						leftServiceNode
						, OpJoin.create( ((OpService)left).getSubOp(), ((OpService)right).getSubOp())
						, false);
			}
	
			// reorder
			if ( compareService( (OpService)left, (OpService)right ) > 0 ) {
				return OpJoin.create( right, left );
			}
		}
		return super.transform(opJoin, left, right);
	}

	@Override
	public Op transform(OpUnion opUnion, Op left, Op right) {
		
		if ( canApplyJoinUnionRule( left, right ) )
            return applyJoinUnionRule( opUnion, left, right );
        else
		// doppelganger #1
		// regra : UNION( S1( x ), S1( y ) ) => S1( UNION (x,y) )
		if ( ( left instanceof OpService && right instanceof OpService) ) {
		
			Node leftServiceNode = ((OpService)left).getService();
			Node rightServiceNode = ((OpService)right).getService();
	
			// merge
			if ( compareService( (OpService)left, (OpService)right ) == 0 ) {
				return new OpService(
						leftServiceNode
						, new OpUnion( ((OpService)left).getSubOp(), ((OpService)right).getSubOp())
						, false);
			}
	
			// reorder
			if ( compareService( (OpService)left, (OpService)right ) > 0 ) {
				return new OpUnion( right, left );
			}
		}
		// regra : UNION( UNION( WHATEVER, S1( x ) ) , S1( y ) ) => UNION( WHATEVER, S1( UNION (x,y) ) )
		// @todo fazer o contrario (left<=>right?)
		if ( left instanceof OpUnion && right instanceof OpService
				&& ((OpUnion)left).getRight() instanceof OpService
				) {
			
			OpUnion leftUnion = ((OpUnion)left);
			OpService rightServ = ((OpService)right);
			OpService leftRightServ = ((OpService)leftUnion.getRight());
			
			// merge
			if ( compareService( leftRightServ, rightServ ) == 0 ) {
				return new OpUnion(
						leftUnion.getLeft(),
						new OpService(
								rightServ.getService(),
								new OpUnion(
										rightServ.getSubOp(),
										leftRightServ.getSubOp()
										),
										false
								)
						);
			}
	
			// reorder
			if ( compareService( leftRightServ, rightServ ) > 0 ) {
				return new OpUnion(
						new OpUnion(
								leftUnion.getLeft(),
								rightServ
								),
							leftRightServ
						);
			}
		}
		// regras: Union( Union( S1(x), S2(y) ), Union( S1(z), S2(w) ) ) => Union(S1(Union(x,z)),S2(Union(y,w)))
		// regras: Union( Union( S1(x), S2(y) ), Union( S1(z), S3(w) ) ) => Union(S1(Union(x,z)),Union(S2(y),S3(w)))
		else if ( left instanceof OpUnion && right instanceof OpUnion ) {

			OpUnion leftUnion = (OpUnion)left;
			OpUnion rightUnion = (OpUnion)right;

			if ( 
					leftUnion.getLeft()  instanceof OpService &&
					leftUnion.getRight() instanceof OpService &&
					rightUnion.getLeft()  instanceof OpService &&
					rightUnion.getRight() instanceof OpService 
				) {
				
				OpService left_left  = (OpService) leftUnion.getLeft();
				OpService left_right = (OpService) leftUnion.getRight();
				OpService right_left  = (OpService) rightUnion.getLeft();
				OpService right_right = (OpService) rightUnion.getRight();

				Op new_left_left = left_left
						, new_left_right = left_right
						, new_right_left = right_left
						, new_right_right = right_right;
				
				// merge lefts

				if ( compareService( left_left , right_left ) == 0 ) {
					new_left_left = 
							new OpService(
									left_left.getService(),
									new OpUnion( left_left.getSubOp(), right_left.getSubOp() ),
									false
									);
					new_right_left = null;
				} else
					// reorder

					if ( compareService( left_left , right_left ) > 0 ) {
						new_left_left = right_left;
						new_right_left = left_left;
				}
				// merge rights

				if ( compareService( left_right, right_right ) == 0 ) {
					new_left_right = 
							new OpService(
									left_right.getService(),
									new OpUnion( left_right.getSubOp(), right_right.getSubOp() ),
									false
									);
					new_right_right = null;
				} else
					// reorder

					if ( compareService( left_right, right_right ) == 0 ) {
						new_left_right  = right_right;
						new_right_right = left_right;
				}
				
				// 
				OpUnion new_left = new OpUnion( new_left_left, new_left_right );
				
				if ( new_right_left == null && new_right_right == null )
					return new_left;
				else if ( new_right_left != null )
					return new OpUnion( new_left, new_right_left );
				else if ( new_right_right != null )
					return new OpUnion( new_left, new_right_right );
				else
					return new OpUnion( new_left, new OpUnion( new_right_left, new_right_right ) );

				// quero ver depurar essa coisa.
				

			}
			
			
		}
		
		
		return super.transform(opUnion, left, right);
	}
	
	private boolean canApplyJoinUnionRule( Op left, Op right ) {
        // System.out.println( "can apply : " + left.toString() + " <=> " + right.toString() );
	    return left instanceof OpJoin && right instanceof OpJoin;
	}
	
	private Op applyJoinUnionRule( OpUnion opUnion, Op left, Op right ) {
	    OpJoin l = (OpJoin) left;
	    OpJoin r = (OpJoin) right;
	    
	    if ( opsAreIdentical( left, right ) ) {
	        return left;
	    }

        if ( opsAreIdentical( l.getLeft(), r.getLeft() ) )
            return OpJoin.create( l.getLeft(), new OpUnion( l.getRight() , r.getRight() ) ) ;
        else
        if ( opsAreIdentical( l.getRight(), r.getLeft() ) )
            return OpJoin.create( l.getRight(), new OpUnion( l.getLeft() , r.getRight() ) ) ;
        else
        if ( opsAreIdentical( l.getRight(), r.getRight() ) )
            return OpJoin.create( l.getRight(), new OpUnion( l.getLeft() , r.getLeft() ) ) ;
	    
	    return super.transform( opUnion, left, right );
	}

    private boolean opsAreIdentical( Op l, Op r ) {
        // @TODO super lazy and probably disfunctional.
        boolean re = l.toString().compareToIgnoreCase( r.toString() ) == 0;
        if ( re )
            System.out.println( "yes it is\n" + l.toString() + "= <=>\n=" + r.toString() + "=" );
        return re;
    }


}
